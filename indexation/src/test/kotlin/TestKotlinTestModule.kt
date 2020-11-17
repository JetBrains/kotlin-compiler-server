import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import common.model.ImportInfo
import indexation.main
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class TestKotlinTestModule {
  @Test
  fun createJsonTest() {
    val pathPrefix = "/Users/Polina.Burtseva/IdeaProjects/kotlin-compiler-server"
//    val testDirectory = "${pathPrefix}/1.4.10-test-kotlin-test"
    val testDirectory = "${pathPrefix}/1.4.10-release-411"
    val testOutput = "${pathPrefix}/hello.json"
    main(arrayOf(testDirectory, testOutput))
    val imports: List<ImportInfo> = jacksonObjectMapper().readValue(File(testOutput).readText())
    val importsText = imports.map{ "import ${it.importName}" }.distinct().joinToString("\n")
    File("${pathPrefix}/indexation/src/test/kotlin/hello.kt").writeText(importsText)
    Files.delete(Paths.get(testOutput))
  }
}