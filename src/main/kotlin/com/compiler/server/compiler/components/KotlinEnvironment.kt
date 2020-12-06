package com.compiler.server.compiler.components

import com.compiler.server.model.bean.LibrariesFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import component.KotlinEnvironment
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.serialization.js.JsModuleDescriptor
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class KotlinEnvironmentConfiguration(val librariesFile: LibrariesFile) {
  @Bean
  fun kotlinEnvironment(): KotlinEnvironment {
    val classPath =
      listOfNotNull(librariesFile.jvm)
        .flatMap {
          it.listFiles()?.toList()
            ?: throw error("No kotlin libraries found in: ${librariesFile.jvm.absolutePath}")
        }

    val additionalJsClasspath = listOfNotNull(librariesFile.js)
    return KotlinEnvironment(classPath, additionalJsClasspath)
  }
}
