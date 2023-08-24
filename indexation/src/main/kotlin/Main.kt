package indexation

/**
 * First argument is path to folder with jars
 * Second argument is path to output file for jvm indexes
 * Third argument is path to output file for js indexes
 */
fun main(args: Array<String>) {
  val (directory, outputPathJvm, outputPathJs, outputPathWasm) = args
  val kotlinEnvironment = KotlinEnvironmentConfiguration(directory).kotlinEnvironment
  JvmIndexationBuilder(kotlinEnvironment = kotlinEnvironment).writeIndexesToFile(outputPathJvm)

  WebIndexationBuilder(
    kotlinEnvironment = kotlinEnvironment,
    configuration = kotlinEnvironment.jsConfiguration,
    libraries = kotlinEnvironment.JS_LIBRARIES
  ).writeIndexesToFile(outputPathJs)

  WebIndexationBuilder(
    kotlinEnvironment = kotlinEnvironment,
    configuration = kotlinEnvironment.wasmConfiguration,
    libraries = kotlinEnvironment.WASM_LIBRARIES
  ).writeIndexesToFile(outputPathWasm)
}
