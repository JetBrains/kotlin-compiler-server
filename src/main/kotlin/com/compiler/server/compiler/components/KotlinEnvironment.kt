package com.compiler.server.compiler.components

import com.compiler.server.model.bean.LibrariesFile
import component.CompilerPluginOption
import component.KotlinEnvironment
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KotlinEnvironmentConfiguration(
  val librariesFile: LibrariesFile
) {
  @Bean
  fun kotlinEnvironment(): KotlinEnvironment {
    val classPath =
      listOfNotNull(librariesFile.jvm)
        .flatMap {
          it.listFiles()?.toList()
            ?: error("No kotlin libraries found in: ${librariesFile.jvm.absolutePath}")
        }

    val additionalJsClasspath = librariesFile.js.listFiles()?.toList() ?: emptyList()
    val additionalWasmClasspath = librariesFile.wasm.listFiles()?.toList() ?: emptyList()
    val additionalComposeWasmClasspath = librariesFile.composeWasm.listFiles()?.toList() ?: emptyList()
    val composeWasmCompilerPlugins = librariesFile.composeWasmComposeCompiler.listFiles()?.toList() ?: emptyList()
    val compilerPlugins = librariesFile.compilerPlugins.listFiles()?.toList() ?: emptyList()

    return KotlinEnvironment(
      classPath,
      additionalJsClasspath,
      additionalWasmClasspath,
      additionalComposeWasmClasspath,
      composeWasmCompilerPlugins,
      compilerPlugins,
      listOf(
        CompilerPluginOption(
          "androidx.compose.compiler.plugins.kotlin",
          "generateDecoys",
          "false"
        ),
      )
    )
  }
}
