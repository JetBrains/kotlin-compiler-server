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
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.kotlinFunction

/**
 * First argument is path to folder with jars
 * Second argument is path to output file
 */
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
private val OBJECT_METHODS = setOf("toString", "equals", "hashCode") // standard object methods

private fun allClassesFromJavaClass(clazz: Class<*>): List<ImportInfo> =
  clazz.classes
    .filter { Modifier.isPublic(it.modifiers) && it.canonicalName != null && it.simpleName.isNotEmpty()}
    .map {
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
    (clazz.kotlin.nestedClasses + clazz.kotlin).filter {
      it.visibility == KVisibility.PUBLIC &&
      it.qualifiedName != null &&
      it.simpleName != null
    }.map {
      val canonicalName = it.qualifiedName!!
      val simpleName = it.simpleName!!
      ImportInfo(
        importName = canonicalName,
        shortName = simpleName,
        fullName = simpleName,
        returnType = simpleName,
        icon = CLASS_ICON)
    }
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
    .flatMap {
      val name = it.name.removeSuffix(CLASS_EXTENSION)
      val fullName = name.replace(File.separator, ".")
      if (fullName.split(".").last() == MODULE_INFO_NAME) return@flatMap emptyList<ImportInfo>()
      val clazz = runCatching { classLoader.loadClass(fullName) }.getOrNull() ?: return@flatMap emptyList<ImportInfo>()
      val classes = if (clazz.isKotlinClass()) {
        allClassesFromKotlinClass(clazz)
      } else {
        allClassesFromJavaClass(clazz)
      }
      val functions = if (!clazz.isInterface) allFunctionsFromClass(clazz) else emptyList()
      classes + functions
    }.distinct()

private fun allFunctionsFromClass(clazz: Class<*>): List<ImportInfo> =
  (clazz.methods + clazz.declaredMethods).distinct()
    .mapNotNull { importInfoFromFunction(it, clazz) }
    .filter { it.shortName !in OBJECT_METHODS }

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
      parametersString = kotlinFunction.parameters.joinToString { "${it.name}: ${kotlinTypeToType(it.type)}" },
      returnType = kotlinTypeToType(kotlinFunction.returnType),
      importPrefix = clazz.`package`.name
    )
  } else if (clazz.isKotlinClass()) null
  else importInfoFromJavaMethod(method, clazz)
}

private fun importInfoFromJavaMethod(method: Method, clazz: Class<*>): ImportInfo? =
  if (Modifier.isPublic(method.modifiers) &&
    Modifier.isStatic(method.modifiers) &&
    !method.isSynthetic &&
    !method.isBridge &&
    clazz.simpleName.isNotEmpty() &&
    method.name.isNotEmpty())
    importInfoByMethodAndParent(
      methodName = method.name,
      parametersString = method.parameters.joinToString { "${it.name}: ${javaTypeToKotlin(it.type)}" },
      returnType = javaTypeToKotlin(method.returnType),
      importPrefix = "${clazz.`package`.name}.${clazz.simpleName}"
    )
  else null

private fun importInfoByMethodAndParent(
  methodName: String,
  parametersString: String,
  returnType: String,
  importPrefix: String
): ImportInfo {
  val shortName = methodName.split("$").first()
  val className = "$shortName($parametersString)"
  val importName = "$importPrefix.$shortName"
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
  val files = File(directoryPath).listFiles()!!.toList()
  val classPathUrls = initClasspath(directoryPath)
  val classLoader = URLClassLoader.newInstance(classPathUrls.toTypedArray())
  File(outputPath).writeText(jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(getAllVariants(classLoader, files)))
}
