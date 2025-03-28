package component

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.jvm.configureAdvancedJvmOptions
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.impl.isKotlinLibrary
import org.jetbrains.kotlin.serialization.js.JsModuleDescriptor
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.io.File

// NOTE: if new class paths are added, please add them to `JavaExec` task's inputs in build.gradle.kts as well
class KotlinEnvironment(
  val classpath: List<File>,
  additionalJsClasspath: List<File>,
  additionalWasmClasspath: List<File>,
  additionalComposeWasmClasspath: List<File>,
  composeWasmCompilerPlugins: List<File>,
  val compilerPlugins: List<File> = emptyList(),
  composeWasmCompilerPluginsOptions: List<CompilerPluginOption>,
) {
  companion object {
    /**
     * This list allows to configure behavior of webdemo compiler. Its effect is equivalent
     * to passing this list of string to CLI compiler.
     *
     * See [org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments] and
     * [org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments] for list of possible flags
     */
    val additionalCompilerArguments: List<String> = listOf(
      "-opt-in=kotlin.ExperimentalStdlibApi",
      "-opt-in=kotlin.time.ExperimentalTime",
      "-opt-in=kotlin.RequiresOptIn",
      "-opt-in=kotlin.ExperimentalUnsignedTypes",
      "-opt-in=kotlin.contracts.ExperimentalContracts",
      "-opt-in=kotlin.experimental.ExperimentalTypeInference",
      "-opt-in=kotlin.uuid.ExperimentalUuidApi",
      "-opt-in=kotlin.io.encoding.ExperimentalEncodingApi",
      "-Xcontext-parameters",
      "-Xnested-type-aliases",
      "-Xreport-all-warnings",
      "-Wextra",
      "-XXLanguage:+ExplicitBackingFields",
    )
  }

  val JS_METADATA_CACHE =
    additionalJsClasspath.flatMap {
      KotlinJavascriptMetadataUtils.loadMetadata(it.absolutePath).map { metadata ->
        val parts = KotlinJavascriptSerializationUtil.readModuleAsProto(metadata.body, metadata.version)
        JsModuleDescriptor(metadata.moduleName, parts.kind, parts.importedModules, parts)
      }
    }

  val JS_LIBRARIES = additionalJsClasspath
    .map { it.absolutePath }
    .filter { isKotlinLibrary(File(it)) }
  val WASM_LIBRARIES = additionalWasmClasspath
    .map { it.absolutePath }
    .filter { isKotlinLibrary(File(it)) }
  val COMPOSE_WASM_LIBRARIES = additionalComposeWasmClasspath
    .map { it.absolutePath }
    .filter { isKotlinLibrary(File(it)) }
  val COMPOSE_WASM_COMPILER_PLUGINS = composeWasmCompilerPlugins
    .map { it.absolutePath }

  val composeWasmCompilerPluginOptions = composeWasmCompilerPluginsOptions
    .map { "plugin:${it.id}:${it.option}=${it.value}" }

  @Synchronized
  fun <T> environment(f: (KotlinCoreEnvironment) -> T): T {
    return f(environment)
  }

  private val configuration = createConfiguration()
  val jsConfiguration: CompilerConfiguration = configuration.copy().apply {
    put(CommonConfigurationKeys.MODULE_NAME, "playground")
    put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
    put(JSConfigurationKeys.LIBRARIES, JS_LIBRARIES)
  }

  val wasmConfiguration: CompilerConfiguration = configuration.copy().apply {
    put(CommonConfigurationKeys.MODULE_NAME, "playground")
    put(JSConfigurationKeys.LIBRARIES, WASM_LIBRARIES)
    put(WasmConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS, false)
    put(WasmConfigurationKeys.WASM_ENABLE_ASSERTS, false)
  }

  val rootDisposable = Disposer.newDisposable()

  val composeWasmConfiguration: CompilerConfiguration = configuration.copy().apply {
    put(CommonConfigurationKeys.MODULE_NAME, "playground")
    put(JSConfigurationKeys.LIBRARIES, COMPOSE_WASM_LIBRARIES)
    put(WasmConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS, false)
    put(WasmConfigurationKeys.WASM_ENABLE_ASSERTS, false)

    PluginCliParser.loadPluginsSafe(
      COMPOSE_WASM_COMPILER_PLUGINS,
      composeWasmCompilerPluginOptions,
      emptyList<String>(),
      this,
      rootDisposable
    )
  }

  private val environment = KotlinCoreEnvironment.createForProduction(
    projectDisposable = rootDisposable,
    configuration = configuration.copy(),
    configFiles = EnvironmentConfigFiles.JVM_CONFIG_FILES
  )

  private fun createConfiguration(): CompilerConfiguration {
    val arguments = K2JVMCompilerArguments()
    parseCommandLineArguments(additionalCompilerArguments, arguments)
    return CompilerConfiguration().apply {
      addJvmClasspathRoots(classpath.filter { it.exists() && it.isFile && it.extension == "jar" })
      val messageCollector = MessageCollector.NONE
      put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
      put(CommonConfigurationKeys.MODULE_NAME, "web-module")
      put(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, true)

      languageVersionSettings = arguments.toLanguageVersionSettings(messageCollector)

      // it uses languageVersionSettings that was set above
      configureAdvancedJvmOptions(arguments)
      put(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT, true)

      configureJdkClasspathRoots()
      val jdkHome = get(JVMConfigurationKeys.JDK_HOME)
      if (jdkHome == null) {
        val javaHome = File(System.getProperty("java.home"))
        put(JVMConfigurationKeys.JDK_HOME, javaHome)
      }
    }
  }
}
