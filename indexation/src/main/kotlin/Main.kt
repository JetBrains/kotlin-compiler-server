package indexation

import common.model.ImportInfo
import com.fasterxml.jackson.module.kotlin.isKotlinClass
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.kotlinFunction

object Main {
  @JvmStatic
    /**
     * First argument is path to folder with jars
     * Second argument is path to output file
     **/
  fun main(args: Array<String>) {
    val directory = args[0]
    val outputPath = args[1]
    createJsonWithIndexes(directory, outputPath)
  }

  private const val MODULE_INFO_NAME = "module-info"
  private const val EXECUTORS_JAR_NAME = "executors.jar"
  private const val JAR_EXTENSION = ".jar"
  private const val LIB_FOLDER_NAME = "lib"
  private const val CLASS_EXTENSION = ".class"
  private const val CLASS_ICON = "class"
  private const val METHOD_ICON = "method"
  private const val TO_STRING_METHOD = "toString"
  private const val EQUALS_METHOD = "equals"
  private const val HASH_CODE_METHOD = "hashCode"
  private const val KOTLIN_TYPE_PREFIX = "(kotlin\\.)([A-Z])" // prefix for simple kotlin type, like Double, Any...

  private fun allClassesFromJavaClass(clazz: Class<*>): List<ImportInfo> =
    clazz.classes.filter {
      Modifier.isPublic(it.modifiers)
    }.map {
      val canonicalName = it.canonicalName
      val simpleName = it.simpleName
      ImportInfo(
        importName = canonicalName,
        shortName = simpleName,
        fullName = simpleName,
        returnType = simpleName,
        icon = CLASS_ICON
      )
    }

  private fun allClassesFromKotlinClass(clazz: Class<*>): List<ImportInfo> =
    runCatching {
      val kotlinClass = clazz.kotlin
      val result = clazz.kotlin.nestedClasses.filter {
        it.visibility == KVisibility.PUBLIC
      }.mapNotNull {
        val canonicalName = it.qualifiedName ?: return@mapNotNull null
        val simpleName = it.simpleName ?: return@mapNotNull null
        ImportInfo(canonicalName, simpleName, simpleName, simpleName, CLASS_ICON)
      }
      val classInfo = if (kotlinClass.visibility == KVisibility.PUBLIC) {
        val canonicalName = kotlinClass.qualifiedName ?: ""
        val simpleName = kotlinClass.simpleName ?: ""
        listOf(
          ImportInfo(
            importName = canonicalName,
            shortName = simpleName,
            fullName = simpleName,
            returnType = simpleName,
            icon = CLASS_ICON
          )
        )
      } else emptyList()
      return@runCatching result + classInfo
    }.getOrDefault(allClassesFromJavaClass(clazz))

  private fun initClasspath(taskRoot: String): List<URL> {
    val cwd = File(taskRoot)
    val classPath = mutableListOf(cwd)
    val rootFiles =
      cwd.listFiles { _: File?, name: String -> name.endsWith(JAR_EXTENSION) || name == LIB_FOLDER_NAME }
        ?: error("No files found from $taskRoot directory")
    for (file in rootFiles) {
      if (file.name == LIB_FOLDER_NAME && file.isDirectory) {
        val libFolderFiles =
          file.listFiles { _: File?, name: String -> name.endsWith(JAR_EXTENSION) } ?: continue
        for (jar in libFolderFiles) {
          classPath.add(jar)
        }
      } else {
        classPath.add(file)
      }
    }
    return classPath.mapNotNull { it.toURI().toURL() }
  }

  private fun getVariantsForZip(classLoader: URLClassLoader, file: File): List<ImportInfo> =
    JarFile(file).entries().toList()
      .filter { !it.isDirectory && it.name.endsWith(CLASS_EXTENSION) }
      .flatMap { entry ->
        val name = entry.name.removeSuffix(CLASS_EXTENSION)
        val fullName = name.replace(File.separator, ".")
        if (fullName == MODULE_INFO_NAME) return@flatMap emptyList<ImportInfo>()
        val clazz = classLoader.loadClass(fullName) ?: return@flatMap emptyList<ImportInfo>()
        val classes = if (clazz.isKotlinClass()) {
          allClassesFromKotlinClass(clazz)
        } else {
          allClassesFromJavaClass(clazz)
        }
        val functions = allFunctionsFromClass(clazz)
        classes + functions
    }.distinct()

  private fun allFunctionsFromClass(clazz: Class<*>): List<ImportInfo> =
    (clazz.methods + clazz.declaredMethods).distinct().mapNotNull { method ->
      importInfoFromFunction(method, clazz)
    }.filter {
      it.shortName != EQUALS_METHOD && it.shortName != HASH_CODE_METHOD && it.shortName != TO_STRING_METHOD
    }

  private fun importInfoFromFunction(method: Method, clazz: Class<*>): ImportInfo? {
    val kotlinFunction = runCatching {
      method.kotlinFunction
    }.getOrNull()
    return if (clazz.isKotlinClass() && kotlinFunction != null &&
      kotlinFunction.visibility == KVisibility.PUBLIC &&
      !kotlinFunction.isExternal
    ) {
      importInfoByMethodAndParent(
        methodName = kotlinFunction.name,
        parametersString = kotlinFunction.parameters.joinToString { kotlinTypeToType(it.type) },
        returnType = kotlinTypeToType(kotlinFunction.returnType),
        parent = clazz
      )
    } else if (clazz.isKotlinClass()) null
    else importInfoFromJavaMethod(method, clazz)
  }

  private fun kotlinTypeToType(kotlinType: KType): String {
    var type = kotlinType.toString()
    val regex = Regex(KOTLIN_TYPE_PREFIX)
    var range: IntRange?
    do {
      range = regex.find(type)?.groups?.get(1)?.range
      type = if (range != null) type.removeRange(range) else type
    } while (range != null)
    return type
  }

  private fun importInfoFromJavaMethod(method: Method, clazz: Class<*>): ImportInfo? =
    if (Modifier.isPublic(method.modifiers) &&
        Modifier.isStatic(method.modifiers) &&
        !method.isSynthetic &&
        !method.isBridge)
      importInfoByMethodAndParent(
        methodName = method.name,
        parametersString = method.parameters.joinToString { it.type.name },
        returnType = method.returnType.simpleName,
        parent = clazz
      )
    else null

  private fun importInfoByMethodAndParent(
    methodName: String,
    parametersString: String,
    returnType: String,
    parent: Class<*>
  ): ImportInfo {
    val shortName = methodName.split("$").first()
    val className = "$shortName($parametersString)"
    val importName = "${parent.`package`.name}.$shortName"
    return ImportInfo(
      importName = importName,
      shortName = shortName,
      fullName = className,
      returnType = returnType,
      icon = METHOD_ICON
    )
  }

  private fun getAllVariants(classLoader: URLClassLoader, files: List<File>): List<ImportInfo> =
    files.filter { jarFile ->
      jarFile.name.split(File.separator).last() != EXECUTORS_JAR_NAME
    }.map { getVariantsForZip(classLoader, it) }.flatten()

  private fun createJsonWithIndexes(directoryPath: String, outputPath: String) {
    val files = File(directoryPath).listFiles().toList()
    val classPathUrls = initClasspath(directoryPath)
    val classLoader = URLClassLoader.newInstance(classPathUrls.toTypedArray())
    File(outputPath).writeText(jacksonObjectMapper().writeValueAsString(getAllVariants(classLoader, files)))
  }
}
