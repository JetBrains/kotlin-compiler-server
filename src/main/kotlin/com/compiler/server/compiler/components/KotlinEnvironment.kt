package com.compiler.server.compiler.components

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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.util.*

@Configuration
class KotlinEnvironmentConfiguration {
  @Bean
  fun kotlinEnvironment() = KotlinEnvironment
    .with(classpath = listOfNotNull(File("lib"))
      .flatMap { it.listFiles().toList() })
}

class KotlinEnvironment(val classpath: List<File>, val coreEnvironment: KotlinCoreEnvironment) {

  companion object {
    /**
     * This list allows to configure behavior of webdemo compiler. Its effect is equivalent
     * to passing this list of string to CLI compiler.
     *
     * See [org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments] and
     * [org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments] for list of possible flags
     */
    private val additionalCompilerArguments: List<String> = listOf(
      "-Xuse-experimental=kotlin.ExperimentalStdlibApi",
      "-Xuse-experimental=kotlin.time.ExperimentalTime",
      "-Xuse-experimental=kotlin.Experimental",
      "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes",
      "-Xuse-experimental=kotlin.contracts.ExperimentalContracts",
      "-Xuse-experimental=kotlin.experimental.ExperimentalTypeInference",
      "-XXLanguage:+InlineClasses"
    )

    fun with(classpath: List<File>): KotlinEnvironment {
      val arguments = K2JVMCompilerArguments()
      parseCommandLineArguments(additionalCompilerArguments, arguments)
      return KotlinEnvironment(classpath, KotlinCoreEnvironment.createForTests(
        parentDisposable = Disposable {},
        extensionConfigs = EnvironmentConfigFiles.JVM_CONFIG_FILES,
        initialConfiguration = CompilerConfiguration().apply {
          addJvmClasspathRoots(classpath.filter { it.exists() && it.isFile && it.extension == "jar" })
          put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
          put(CommonConfigurationKeys.MODULE_NAME, UUID.randomUUID().toString())
          with(K2JVMCompilerArguments()) {
            put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, noParamAssertions)
            put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, noCallAssertions)
          }
          languageVersionSettings = arguments.toLanguageVersionSettings(this[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]!!)
        }
      ))
    }
  }
}
