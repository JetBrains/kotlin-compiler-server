package indexation

/**
 * First argument is path to folder with jars
 * Second argument is path to output file for jvm indexes
 * Third argument is path to output file for js indexes
 */
fun main(args: Array<String>) {
  val (directory, outputPathJvm, outputPathJs) = args
  createJsonWithIndexes(directory, outputPathJvm)
  createJsonWithIndexesJS(directory, outputPathJs)
}
