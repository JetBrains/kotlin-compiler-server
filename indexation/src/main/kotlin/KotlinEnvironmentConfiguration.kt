package indexation

import component.KotlinEnvironment
import java.io.File

class KotlinEnvironmentConfiguration(fileName: String) {
  val kotlinEnvironment = run {
    val jvmFile = File(fileName)
    val jsFile = File("$fileName-js")
    val wasmFile = File("$fileName-wasm")
    val classPath =
      listOfNotNull(jvmFile)
        .flatMap {
          it.listFiles()?.toList()
            ?: error("No kotlin libraries found in: ${jvmFile.absolutePath}")
        }

    val additionalJsClasspath = jsFile.listFiles()?.toList() ?: emptyList()
    val additionalWasmClasspath = wasmFile.listFiles()?.toList() ?: emptyList()

    KotlinEnvironment(classPath, additionalJsClasspath, additionalWasmClasspath)
  }
}
