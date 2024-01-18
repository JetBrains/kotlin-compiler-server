package indexation

/**
 * First argument is path to folder with jars
 * Second argument is path to output file for jvm indexes
 * Third argument is path to output file for js indexes
 */
fun main(args: Array<String>) {
  val (version, directory, outputPathJvm, outputPathJs, outputPathWasm) = args
  val kotlinEnvironment = KotlinEnvironmentConfiguration(version, directory).kotlinEnvironment
  JvmIndexationBuilder(kotlinEnvironment = kotlinEnvironment).writeIndexesToFile(outputPathJvm)

  WebIndexationBuilder(
    kotlinEnvironment = kotlinEnvironment,
    configuration = kotlinEnvironment.jsConfiguration,
    libraries = kotlinEnvironment.JS_LIBRARIES,
    compilerPlugins = false
  ).writeIndexesToFile(outputPathJs)

  WebIndexationBuilder(
    kotlinEnvironment = kotlinEnvironment,
    configuration = kotlinEnvironment.wasmConfiguration,
    libraries = kotlinEnvironment.WASM_LIBRARIES,
    compilerPlugins = true
  ).writeIndexesToFile(outputPathWasm)
}
