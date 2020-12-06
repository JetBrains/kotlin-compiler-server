package indexation

/**
 * First argument is path to folder with jars
 * Second argument is path to output file for jvm indexes
 * Third argument is path to output file for js indexes
 */
fun main(args: Array<String>) {
  val (directory, outputPathJvm, outputPathJs) = args
  val kotlinEnvironmentConfiguration = KotlinEnvironmentConfiguration(directory)
  JvmIndexationBuilder(directoryPath = directory).createIndexes(outputPathJvm)
  JsIndexationBuilder(kotlinEnvironment = kotlinEnvironmentConfiguration.kotlinEnvironment).createIndexes(outputPathJs)
}
