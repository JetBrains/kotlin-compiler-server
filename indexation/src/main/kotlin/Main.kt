package indexation

/**
 * First argument is path to folder with jars
 * Second argument is path to output file for jvm indexes
 * Third argument is path to output file for js indexes
 */
fun main(args: Array<String>) {
  val directory = args[0]
  val outputPathJvm = args[1]
  val outputPathJs = args[2]
  createJsonWithIndexes(directory, outputPathJvm)
  createJsonWithIndexesJS(directory, outputPathJs)
}
