package indexation

import component.KotlinEnvironment
import java.io.File

class KotlinEnvironmentConfiguration(fileName: String) {
  val kotlinEnvironment = run {
    val jvmFile = File(fileName)
    val jsFile = File("$fileName-js")
    val compilerPluginFile = File("$fileName-compiler-plugins")
    val classPath =
      listOfNotNull(jvmFile)
        .flatMap {
          it.listFiles()?.toList()
            ?: error("No kotlin libraries found in: ${jvmFile.absolutePath}")
        }

    val additionalJsClasspath = listOfNotNull(jsFile)
    val compilerPlugins = listOfNotNull(compilerPluginFile)
      .flatMap {
        it.listFiles()?.toList() ?: arrayListOf()
      }

    KotlinEnvironment(classPath, additionalJsClasspath, compilerPlugins)
  }
}
