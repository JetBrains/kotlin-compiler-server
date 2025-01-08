package com.compiler.server.common.components

import component.CompilerPluginOption
import java.io.File

class KotlinEnvironmentConfiguration(
  version: String,
  fileName: String
) {
  val kotlinEnvironment = run {
    val jvmFile = File(fileName)
    val jsFile = File("$fileName-js")
    val wasmFile = File("$fileName-wasm")
    val composeWasmFile = File("$fileName-compose-wasm")
    val composeWasmCompilerPluginsFile = File("$fileName-compose-wasm-compiler-plugins")
    val composeWasmCachesFile = File("$fileName-caches-compose-wasm")
    val classPath =
      listOfNotNull(jvmFile)
        .flatMap {
          it.listFiles()?.toList()
            ?: error("No kotlin libraries found in: ${jvmFile.absolutePath}")
        }

    val additionalJsClasspath = jsFile.listFiles()?.toList() ?: emptyList()
    val additionalWasmClasspath = wasmFile.listFiles()?.toList() ?: emptyList()
    val additionalComposeWasmClasspath = composeWasmFile.listFiles()?.toList() ?: emptyList()
    val composeWasmCompilerPlugins = composeWasmCompilerPluginsFile.listFiles()?.toList() ?: emptyList()

    KotlinEnvironment(
      classPath,
      additionalJsClasspath,
      additionalWasmClasspath,
      additionalComposeWasmClasspath,
      composeWasmCompilerPlugins,
      emptyList(),
      listOf(
        CompilerPluginOption(
          "androidx.compose.compiler.plugins.kotlin",
          "generateDecoys",
          "false"
        ),
      ),
      composeWasmCachesFile
    )
  }
}
