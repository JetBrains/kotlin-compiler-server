package indexation

/**
 * First argument is path to folder with jars
 * Second argument is path to output file
 */
fun main(args: Array<String>) {
  val directory = args[0]
  val outputPath = args[1]
  val outputPathJs = args[2]
  createJsonWithIndexes(directory, outputPath)
  createJsonWithIndexesJS(directory, outputPathJs)
}
