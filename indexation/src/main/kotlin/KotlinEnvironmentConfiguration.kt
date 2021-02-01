package indexation

import component.KotlinEnvironment
import java.io.File

class KotlinEnvironmentConfiguration(fileName: String) {
  val kotlinEnvironment: KotlinEnvironment
  init {
    val jvmFile = File(fileName)
    val jsFile = File("$fileName-js")
    val classPath =
      listOfNotNull(jvmFile)
        .flatMap {
          it.listFiles()?.toList()
            ?: throw error("No kotlin libraries found in: ${jvmFile.absolutePath}")
        }

    val additionalJsClasspath = listOfNotNull(jsFile)
    kotlinEnvironment = KotlinEnvironment(classPath, additionalJsClasspath)
  }
}
