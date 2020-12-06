package indexation

import component.KotlinEnvironment
import java.io.File

class KotlinEnvironmentConfiguration(fileName: String) {
  private val jvmFile = File(fileName)
  private val jsFile = File("$fileName-js")
  fun kotlinEnvironment(): KotlinEnvironment {
    val classPath =
      listOfNotNull(jvmFile)
        .flatMap {
          it.listFiles()?.toList()
            ?: throw error("No kotlin libraries found in: ${jvmFile.absolutePath}")
        }

    val additionalJsClasspath = listOfNotNull(jsFile)
    return KotlinEnvironment(classPath, additionalJsClasspath)
  }
}
