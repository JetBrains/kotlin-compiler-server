package component

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JsIrCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.jvm.configureAdvancedJvmOptions
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.jsPhases
import org.jetbrains.kotlin.ir.backend.js.jsResolveLibraries
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.serialization.js.JsModuleDescriptor
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.util.Logger
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.File

class KotlinEnvironment(
  val classpath: List<File>,
  additionalJsClasspath: List<File>
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
      "-opt-in=kotlin.ExperimentalStdlibApi",
      "-opt-in=kotlin.time.ExperimentalTime",
      "-opt-in=kotlin.RequiresOptIn",
      "-opt-in=kotlin.ExperimentalUnsignedTypes",
      "-opt-in=kotlin.contracts.ExperimentalContracts",
      "-opt-in=kotlin.experimental.ExperimentalTypeInference",
      "-Xcontext-receivers",
      "-XXLanguage:+RangeUntilOperator"
    )
  }

  val JS_METADATA_CACHE =
    additionalJsClasspath.flatMap {
      KotlinJavascriptMetadataUtils.loadMetadata(it.absolutePath).map { metadata ->
        val parts = KotlinJavascriptSerializationUtil.readModuleAsProto(metadata.body, metadata.version)
        JsModuleDescriptor(metadata.moduleName, parts.kind, parts.importedModules, parts)
      }
    }

  val JS_LIBRARIES = additionalJsClasspath.map { it.absolutePath }

  @Synchronized
  fun <T> environment(f: (KotlinCoreEnvironment) -> T): T {
    return f(environment)
  }

  private val configuration = createConfiguration()
  val jsConfiguration: CompilerConfiguration = configuration.copy().apply {
    put(CommonConfigurationKeys.MODULE_NAME, "moduleId")
    put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
    put(JSConfigurationKeys.LIBRARIES, JS_LIBRARIES)
  }

  private val messageCollector = object : MessageCollector {
    override fun clear() {}
    override fun hasErrors(): Boolean {
      return false
    }

    override fun report(
      severity: CompilerMessageSeverity,
      message: String,
      location: CompilerMessageSourceLocation?
    ) {
    }
  }

  val jsIrPhaseConfig = createPhaseConfig(jsPhases, K2JsIrCompiler().createArguments(), messageCollector)

  val jsIrResolvedLibraries = jsResolveLibraries(
    JS_LIBRARIES,
    emptyList(),
    object : Logger {
      override fun error(message: String) {}
      override fun fatal(message: String): Nothing {
        TODO("Fake logger for compiler server")
      }

      override fun log(message: String) {}
      override fun warning(message: String) {}
    }
  )

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
      put(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, true)
      put(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, true)

      languageVersionSettings = arguments.toLanguageVersionSettings(messageCollector)

      // it uses languageVersionSettings that was set above
      configureAdvancedJvmOptions(arguments)
      put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, true)

      configureJdkClasspathRoots()
    }
  }
}