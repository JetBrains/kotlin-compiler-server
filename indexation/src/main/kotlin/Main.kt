package indexation

/**
 * First argument is path to folder with jars
 * Second argument is path to output file for jvm indexes
 * Third argument is path to output file for js indexes
 */
fun main(args: Array<String>) {
  val version = args[0]
  val directory = args[1]
  val outputPathJvm = args[2]
  val outputPathJs = args[3]
  val outputPathWasm = args[4]
  val outputPathComposeWasm = args[5]
  val kotlinEnvironment = KotlinEnvironmentConfiguration(version, directory).kotlinEnvironment
  JvmIndexationBuilder(kotlinEnvironment = kotlinEnvironment).writeIndexesToFile(outputPathJvm)

  WebIndexationBuilder(
    kotlinEnvironment = kotlinEnvironment,
    inputConfiguration = kotlinEnvironment.jsConfiguration,
    libraries = kotlinEnvironment.JS_LIBRARIES,
    compilerPlugins = emptyList(),
    compilerPluginOptions = emptyList(),
    platformConfiguration = kotlinEnvironment.jsConfiguration
  ).writeIndexesToFile(outputPathJs)

  WebIndexationBuilder(
    kotlinEnvironment = kotlinEnvironment,
    inputConfiguration = kotlinEnvironment.wasmConfiguration,
    libraries = kotlinEnvironment.WASM_LIBRARIES,
    compilerPlugins = emptyList(),
    compilerPluginOptions = emptyList(),
    platformConfiguration = kotlinEnvironment.wasmConfiguration
  ).writeIndexesToFile(outputPathWasm)

  WebIndexationBuilder(
    kotlinEnvironment = kotlinEnvironment,
    inputConfiguration = kotlinEnvironment.composeWasmConfiguration,
    libraries = kotlinEnvironment.COMPOSE_WASM_LIBRARIES,
    compilerPlugins = kotlinEnvironment.COMPOSE_WASM_COMPILER_PLUGINS,
    compilerPluginOptions = kotlinEnvironment.composeWasmCompilerPluginOptions,
    platformConfiguration = kotlinEnvironment.composeWasmConfiguration
  ).writeIndexesToFile(outputPathComposeWasm)
}
