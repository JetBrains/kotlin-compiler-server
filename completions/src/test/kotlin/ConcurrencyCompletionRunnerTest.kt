import base.BaseCompletionTest
import base.BaseCompletionTest.Utils.retrieveCompletions
import completions.dto.api.Completion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import lsp.utils.CARET_MARKER
import lsp.utils.KotlinLspComposeExtension
import lsp.utils.extractCaret
import org.eclipse.lsp4j.Position
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import kotlin.test.Ignore
import kotlin.test.assertTrue

abstract class ConcurrencyCompletionRunnerTest : BaseCompletionTest {
    @Test
    fun `a lot of complete test`() {
        runManyTest {
            performCompletionChecks(
                codeWithCaret = "fun main() {\n    val alex = 1\n    val alex1 = 1 + a$CARET_MARKER\n}",
                expected = listOf(
                    "alex"
                )
            )
        }
    }

    // TODO(Dmitrii Krasnov): this test is disabled until KTL-2807 is fixed
    @Ignore
    @Test
    fun `a lot of complete test JS`() {
        runManyTest {
            performCompletionChecks(
                codeWithCaret = "fun main() {\n    val alex = 1\n    val alex1 = 1 + a$CARET_MARKER\n}",
                expected = listOf(
                    "alex"
                ),
                isJs = true
            )
        }
    }

    private fun runManyTest(times: Int = 100, test: () -> Unit) {
        runBlocking {
            launch(Dispatchers.IO) {
                repeat(times) {
                    launch(Dispatchers.IO) {
                        test()
                    }
                }
            }.join()
        }
    }
}

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [completions.CompletionsApplication::class]
)
@ExtendWith(KotlinLspComposeExtension::class)
class StatelessConcurrencyCompletionRunnerTest : ConcurrencyCompletionRunnerTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var webTestClient: WebTestClient

    override fun performCompletionChecks(
        codeWithCaret: String,
        expected: List<String>,
        isJs: Boolean
    ) {
        assumeFalse(isJs, "JS completions are not supported by LSP yet.")
        val (code, caret) = extractCaret { codeWithCaret }
        val completions = retrieveCompletionsFromEndpoint(code, caret).map { it.displayText }
        assertAll(expected.map { exp ->
            { assertTrue(completions.any { it.contains(exp) }, "Expected completion $exp but got $completions") }
        })
    }

    private fun retrieveCompletionsFromEndpoint(code: String, position: Position): List<Completion>  {
        val url = "http://localhost:$port/api/compiler/complete?line=${position.line}&ch=${position.character}"
        return webTestClient.retrieveCompletions(url, code)
    }

}