package indexation

import component.CompilerPluginOption
import component.KotlinEnvironment
import java.io.File

class KotlinEnvironmentConfiguration(
  version: String,
  fileName: String
) {
  val kotlinEnvironment = run {
    val jvmFile = File(fileName)
    val jsFile = File("$fileName-js")
    val wasmFile = File("$fileName-wasm")
    val compilerPluginsFile = File("$fileName-compiler-plugins")
    val classPath =
      listOfNotNull(jvmFile)
        .flatMap {
          it.listFiles()?.toList()
            ?: error("No kotlin libraries found in: ${jvmFile.absolutePath}")
        }

    val additionalJsClasspath = jsFile.listFiles()?.toList() ?: emptyList()
    val additionalWasmClasspath = wasmFile.listFiles()?.toList() ?: emptyList()
    val compilerPlugins = compilerPluginsFile.listFiles()?.toList() ?: emptyList()

    KotlinEnvironment(
      classPath,
      additionalJsClasspath,
      additionalWasmClasspath,
      compilerPlugins,
      listOf(
        CompilerPluginOption(
          "androidx.compose.compiler.plugins.kotlin",
          "generateDecoys",
          "false"
        ),
        CompilerPluginOption(
          "androidx.compose.compiler.plugins.kotlin",
          "suppressKotlinVersionCompatibilityCheck",
          version
        ),
      )
    )
  }
}
