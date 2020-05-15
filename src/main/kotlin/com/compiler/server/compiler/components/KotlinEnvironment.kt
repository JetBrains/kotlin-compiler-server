package com.compiler.server.compiler.components

import com.compiler.server.model.bean.LibrariesFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
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

class KotlinEnvironment(
  val classpath: List<File>,
  additionalJsClaspath: List<File>
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
      "-Xuse-experimental=kotlin.ExperimentalStdlibApi",
      "-Xuse-experimental=kotlin.time.ExperimentalTime",
      "-Xuse-experimental=kotlin.Experimental",
      "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes",
      "-Xuse-experimental=kotlin.contracts.ExperimentalContracts",
      "-Xuse-experimental=kotlin.experimental.ExperimentalTypeInference",
      "-XXLanguage:+InlineClasses"
    )
  }

  val JS_METADATA_CACHE =
    additionalJsClaspath.flatMap {
      KotlinJavascriptMetadataUtils.loadMetadata(it.absolutePath).map { metadata ->
        val parts = KotlinJavascriptSerializationUtil.readModuleAsProto(metadata.body, metadata.version)
        JsModuleDescriptor(metadata.moduleName, parts.kind, parts.importedModules, parts)
      }
    }

  val JS_LIBRARIES = additionalJsClaspath.map { it.absolutePath }

  @Synchronized
  fun <T> environment(f: (KotlinCoreEnvironment) -> T): T {
    return f(environment)
  }

  private val configuration = createConfiguration()
  val jsConfiguration = configuration.copy().apply {
    put(CommonConfigurationKeys.MODULE_NAME, "moduleId")
    put(JSConfigurationKeys.LIBRARIES, JS_LIBRARIES)
  }

  private val environment = KotlinCoreEnvironment.createForProduction(
          parentDisposable = Disposer.newDisposable(),
          configuration = configuration.copy(),
          configFiles = EnvironmentConfigFiles.JVM_CONFIG_FILES
  )

  private fun createConfiguration(): CompilerConfiguration {
    val arguments = K2JVMCompilerArguments()
    parseCommandLineArguments(additionalCompilerArguments, arguments)
    return CompilerConfiguration().apply {
      addJvmClasspathRoots(classpath.filter { it.exists() && it.isFile && it.extension == "jar" })
      val messageCollector = MessageCollector.NONE
      put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
      put(CommonConfigurationKeys.MODULE_NAME, "web-module")
      with(K2JVMCompilerArguments()) {
        put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, noParamAssertions)
        put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, noCallAssertions)
        put(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, true)
      }
      languageVersionSettings = arguments.toLanguageVersionSettings(messageCollector)
    }
  }
}
