package indexation

import component.KotlinEnvironment
import java.io.File

class KotlinEnvironmentConfiguration(fileName: String) {
  val kotlinEnvironment = run {
    val jvmFile = File(fileName)
    val jsFile = File("$fileName-js")
    val cachesJsDir = File("$fileName-js-caches")
    val classPath =
      listOfNotNull(jvmFile)
        .flatMap {
          it.listFiles()?.toList()
            ?: error("No kotlin libraries found in: ${jvmFile.absolutePath}")
        }

    val additionalJsClasspath = listOfNotNull(jsFile)
    KotlinEnvironment(classPath, additionalJsClasspath, cachesJsDir)
  }
}
