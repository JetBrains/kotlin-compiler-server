package indexation

import com.fasterxml.jackson.module.kotlin.isKotlinClass
import com.google.gson.Gson
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.kotlinFunction

object Main {
  private data class ImportInfo(val importName: String, val shortName: String, val fullName: String)

  private var INDEXES_FILE_NAME = "output.json"
  private val MODULE_INFO_NAME = "module-info"
  private val EXECUTORS_JAR_NAME = "executors.jar"

  private fun allClassesFromJavaClass(clazz: Class<*>): HashSet<ImportInfo> {
    val allClasses = hashSetOf<ImportInfo>()
    clazz.classes.filter {
      Modifier.isPublic(it.modifiers)
    }.map {
      val canonicalName = it.canonicalName
      val simpleName = it.simpleName
      val importInfo = ImportInfo(canonicalName, simpleName, simpleName)
      allClasses.add(importInfo)
    }
    return allClasses
  }

  private fun allClassesFromKotlinClass(clazz: Class<*>): HashSet<ImportInfo> {
    val allClasses = hashSetOf<ImportInfo>()
    val kotlinClass = clazz.kotlin
    try {
      kotlinClass.nestedClasses.filter {
        it.visibility == KVisibility.PUBLIC
      }.map {
        val canonicalName = it.qualifiedName ?: ""
        val simpleName = it.simpleName ?: ""
        val importInfo = ImportInfo(canonicalName, simpleName, simpleName)
        allClasses.add(importInfo)
      }
      if (kotlinClass.visibility == KVisibility.PUBLIC) {
        val canonicalName = kotlinClass.qualifiedName ?: ""
        val simpleName = kotlinClass.simpleName ?: ""
        val importInfo = ImportInfo(canonicalName, simpleName, simpleName)
        allClasses.add(importInfo)
      }
    } catch (exception: UnsupportedOperationException) {
      // throw kotlinClass.nestedClasses
      return allClassesFromJavaClass(clazz)
    } catch (error: AssertionError) {
      /* In Tests:
    1) kotlinx.coroutines.flow.internal.ChannelFlowKt$withContextUndispatched$$inlined
      $suspendCoroutineUninterceptedOrReturn$lambda$1
    2) kotlin.coroutines.experimental.intrinsics.IntrinsicsKt__IntrinsicsJvmKt
      $createCoroutineUnchecked$$inlined$buildContinuationByInvokeCall$IntrinsicsKt__IntrinsicsJvmKt$1
    3) kotlin.coroutines.experimental.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnchecked
      $$inlined$buildContinuationByInvokeCall$IntrinsicsKt__IntrinsicsJvmKt$2
    */
      return allClassesFromJavaClass(clazz)
    } catch (error: IncompatibleClassChangeError) {
      /* In Application:
    1) kotlinx.coroutines.CoroutineDispatcher$Key
    2) kotlinx.coroutines.channels.ConflatedChannel$Companion
    3) kotlinx.coroutines.ExecutorCoroutineDispatcher$Key
    */
      return allClassesFromJavaClass(clazz)
    } catch (error: InternalError) {
      /* In Application:
    1-53) kotlinx.coroutines.flow.FlowKt__*
    54-59) kotlinx.coroutines.channels.*
     */
      return allClassesFromJavaClass(clazz)
    } catch (exception: NoSuchElementException) {
      /* In Application:
    1) kotlinx.coroutines.flow.internal.ChannelFlowKt$withContextUndispatched$$inlined
      $suspendCoroutineUninterceptedOrReturn$lambda$1
    2) kotlin.coroutines.experimental.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnchecked
      $$inlined$buildContinuationByInvokeCall$IntrinsicsKt__IntrinsicsJvmKt$1
    3) kotlin.coroutines.experimental.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnchecked
      $$inlined$buildContinuationByInvokeCall$IntrinsicsKt__IntrinsicsJvmKt$2
    */
      return allClassesFromJavaClass(clazz)
    }
    return allClasses
  }

  private const val JAR_EXTENSION = ".jar"
  private const val LIB_FOLDER_NAME = "lib"

  private fun initClasspath(taskRoot: String): List<URL> {
    val cwd = File(taskRoot)
    val classPath = mutableListOf(File(taskRoot)) // Add top level folders
    // Find any Top level jars or jars in the lib folder
    val rootFiles =
      cwd.listFiles { _: File?, name: String -> name.endsWith(JAR_EXTENSION) || name == LIB_FOLDER_NAME }
        ?: error("No files found from $taskRoot directory")
    for (file in rootFiles) {
      if (file.name == LIB_FOLDER_NAME && file.isDirectory) {
        // Collect all Jars in /lib directory
        val libFolderFiles =
          file.listFiles { _: File?, name: String -> name.endsWith(JAR_EXTENSION) } ?: continue
        for (jar in libFolderFiles) {
          classPath.add(jar)
        }
      } else {
        // Add top level dirs and jar files
        classPath.add(file)
      }
    }
    // Convert Files to URLs
    return classPath.mapNotNull { it.toURI().toURL() }
  }

  private fun getHandlerClass(taskRoot: String, className: String): Class<*>? {
    val classPathUrls = initClasspath(taskRoot)
    val cl = URLClassLoader.newInstance(classPathUrls.toTypedArray())
    return cl.loadClass(className)
  }

  private fun getVariantsForZip(directory: String, file: File): List<ImportInfo> {
    val jarFile = JarFile(file)
    val allSuggests = hashSetOf<ImportInfo>()
    jarFile.entries().toList().map { entry ->
      if (!entry.isDirectory
        && entry.name.endsWith(".class")
      ) {
        val name = entry.name.removeSuffix(".class")
        val fullName = name.replace("/", ".")
        if (fullName != MODULE_INFO_NAME) {
          try {
//            val clazz = ClassLoader.getSystemClassLoader().loadClass(fullName)
            val clazz = getHandlerClass(directory, fullName) ?: return emptyList()
            if (clazz.isKotlinClass()) {
              allSuggests.addAll(allClassesFromKotlinClass(clazz))
            } else {
              allSuggests.addAll(allClassesFromJavaClass(clazz))
            }
            allSuggests.addAll(allFunctionsFromClass(clazz))
          } catch (error: VerifyError) {
            /* In Application:
            1) kotlinx.coroutines.EventLoopImplBase$DelayedTaskQueue
            2) kotlinx.coroutines.flow.internal.FlowProduceCoroutine
            */
          } catch (error: NoClassDefFoundError) {
            /* In Application:
          1) kotlinx.coroutines.flow.FlowKt__MergeKt$flatMapLatest$1
          2) kotlinx.coroutines.flow.FlowKt__MergeKt$mapLatest$1
          3) kotlinx.coroutines.flow.FlowKt__MergeKt$flattenConcat$$inlined$unsafeFlow$1
          4) kotlinx.coroutines.flow.FlowKt__MergeKt$flatMapConcat$$inlined$map$1
          5) kotlinx.coroutines.flow.FlowKt__MergeKt$flatMapMerge$$inlined$map$1
           */
          }
        }
      }
    }
    return allSuggests.toList()
  }

  private fun allFunctionsFromClass(clazz: Class<*>): HashSet<ImportInfo> {
    val allClasses = hashSetOf<ImportInfo>()
    clazz.declaredMethods.map { method ->
      val importInfo = importInfoFromFunction(method, clazz)
      if (importInfo != null) allClasses.add(importInfo)
    }
    return allClasses
  }

  private fun importInfoFromFunction(method: Method, clazz: Class<*>): ImportInfo? {
    var kotlinFunction: KFunction<*>? = null
    try {
      kotlinFunction = method.kotlinFunction
    } catch (exception: NoSuchElementException) {
    } catch (exception: UnsupportedOperationException) {
    } catch (error: KotlinReflectionInternalError) {
    } catch (error: AssertionError) {
    } catch (error: InternalError) {
    } catch (error: IncompatibleClassChangeError) {
    }
    return if (kotlinFunction != null
      && kotlinFunction.visibility == KVisibility.PUBLIC) {
      importInfoByMethodAndParent(
        kotlinFunction.name,
        kotlinFunction.parameters.map { it.type }.joinToString(),
        clazz)
    } else importInfoFromJavaMethod(method, clazz)
  }

  private fun importInfoFromJavaMethod(method: Method, clazz: Class<*>): ImportInfo? =
    if (Modifier.isPublic(method.modifiers) &&
      Modifier.isStatic(method.modifiers))
      importInfoByMethodAndParent(method.name, method.parameters.map { it.type.name }.joinToString(), clazz)
    else null

  private fun importInfoByMethodAndParent(methodName: String, parametersString: String, parent: Class<*>): ImportInfo {
    val shortName = methodName.split("$").first()
    val className = "$shortName($parametersString)"
    val importName = "${parent.`package`.name}.$shortName"
    return ImportInfo(importName, shortName, className)
  }

  private fun getAllVariants(dir:String, files: List<File>): List<ImportInfo> {
    val jarFiles = files.filter { jarFile ->
      jarFile.name.split("/").last() != EXECUTORS_JAR_NAME
    }
    val allVariants = mutableListOf<ImportInfo>()
    jarFiles.map { file ->
      val variants = getVariantsForZip(dir, file)
      allVariants.addAll(variants)
    }
    return allVariants
  }

  private fun createJsonWithIndexes(directoryPath: String) {
    val file = File(directoryPath)
    val filesArr = file.listFiles()
    val files = filesArr.toList()
    File(INDEXES_FILE_NAME).writeText("")
    File(INDEXES_FILE_NAME).appendText(Gson().toJson(getAllVariants(directoryPath, files)))
//    File(INDEXES_FILE_NAME).appendText(GsonBuilder().setPrettyPrinting().create().toJson(getAllVariants()))
  }

  @JvmStatic
  fun main(args: Array<String>) {
    val directory = args[0]
    INDEXES_FILE_NAME = args[1]
//    getHandlerClass(directory, "kotlin.jdk7.AutoCloseableKt")
    createJsonWithIndexes(directory)
  }
}
