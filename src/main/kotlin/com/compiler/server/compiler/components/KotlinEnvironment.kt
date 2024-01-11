package com.compiler.server.compiler.components

import com.compiler.server.model.bean.LibrariesFile
import com.compiler.server.model.bean.VersionInfo
import component.CompilerPluginOption
import component.KotlinEnvironment
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KotlinEnvironmentConfiguration(
  val versionInfo: VersionInfo,
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
    val compilerPlugins = librariesFile.composeCompiler.listFiles()?.toList() ?: emptyList()

    return KotlinEnvironment(
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
          versionInfo.version
        ),
      )
    )
  }
}
