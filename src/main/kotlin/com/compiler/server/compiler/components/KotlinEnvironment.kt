package com.compiler.server.compiler.components

import com.compiler.server.model.bean.LibrariesFile
import com.intellij.openapi.Disposable
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.util.*

@Configuration
class KotlinEnvironmentConfiguration(val librariesFile: LibrariesFile) {
  @Bean
  fun kotlinEnvironment() = KotlinEnvironment
    .with(
      classpath = listOfNotNull(librariesFile.jvm)
        .flatMap { it.listFiles()?.toList() ?: throw error("No kotlin libraries found in: ${librariesFile.jvm.absolutePath}") },
      classpathJs = listOfNotNull(librariesFile.js)
    )
}

class KotlinEnvironment(
  val classpath: List<File>,
  val coreEnvironment: KotlinCoreEnvironment,
  val jsEnvironment: CompilerConfiguration
) {

  companion object {
    /**
     * This list allows to configure behavior of webdemo compiler. Its effect is equivalent
     * to passing this list of string to CLI compiler.
     *
     * See [org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments] and
     * [org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments] for list of possible flags
     */
    private val additionalCompilerArguments: List<String> = listOf(
      "-Xopt-in=kotlin.ExperimentalStdlibApi",
      "-Xopt-in=kotlin.time.ExperimentalTime",
      "-Xopt-in=kotlin.RequiresOptIn",
      "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
      "-Xopt-in=kotlin.contracts.ExperimentalContracts",
      "-Xopt-in=kotlin.experimental.ExperimentalTypeInference",
      "-XXLanguage:+InlineClasses"
    )

    fun with(classpath: List<File>, classpathJs: List<File>): KotlinEnvironment {
      val arguments = K2JVMCompilerArguments()
      parseCommandLineArguments(additionalCompilerArguments, arguments)
      val coreEnvironment = KotlinCoreEnvironment.createForTests(
        parentDisposable = Disposable {},
        extensionConfigs = EnvironmentConfigFiles.JVM_CONFIG_FILES,
        initialConfiguration = CompilerConfiguration().apply {
          addJvmClasspathRoots(classpath.filter { it.exists() && it.isFile && it.extension == "jar" })
          put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
          put(CommonConfigurationKeys.MODULE_NAME, UUID.randomUUID().toString())
          with(K2JVMCompilerArguments()) {
            put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, noParamAssertions)
            put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, noCallAssertions)
            put(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, true)
          }
          languageVersionSettings = arguments.toLanguageVersionSettings(this[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]!!)
        }
      )
      val jsEnvironment = coreEnvironment.configuration.copy().apply {
        put(CommonConfigurationKeys.MODULE_NAME, "moduleId")
        put(JSConfigurationKeys.LIBRARIES, classpathJs.map { it.absolutePath })
      }
      return KotlinEnvironment(classpath, coreEnvironment, jsEnvironment)
    }
  }
}
