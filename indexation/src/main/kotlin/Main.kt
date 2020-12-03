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
  val outputPathJs = args[2]
  createJsonWithIndexes(directory, outputPath)
  createJsonWithIndexesJS(directory, outputPathJs)
}
