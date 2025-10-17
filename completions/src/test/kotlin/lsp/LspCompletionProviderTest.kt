package lsp

import CompletionTest
import base.BaseCompletionTest.Utils.retrieveCompletions
import lsp.utils.CARET_MARKER
import lsp.utils.KotlinLspComposeExtension
import lsp.utils.extractCaret
import completions.dto.api.Completion
import completions.dto.api.Icon
import org.eclipse.lsp4j.Position
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [completions.CompletionsApplication::class]
)
@ExtendWith(KotlinLspComposeExtension::class)
class LspCompletionProviderTest : CompletionTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var webTestClient: WebTestClient

    val testCode = """
        fun main() {
            3.0.toIn$CARET_MARKER
        }
        fun Double.toIntervalZeroBased(): IntRange = IntRange(0, this.toInt())
    """.trimIndent()

    @Test
    fun `rest endpoint should return simple completions`() {
        val completions = getCompletions(testCode)
        val toUint = completions.find { it.text == "toUInt()" }
            ?: error("Expected to find \"toUInt()\" completion, but got $completions\"")

        val expected = Completion(
            text = "toUInt()",
            displayText = "toUInt() for Double in kotlin",
            tail = "UInt",
            icon = Icon.METHOD,
            hasOtherImports = null
        )
        assertEquals(expected, toUint)
    }

    @Test
    fun `rest endpoint should return completions in the expected order`() {
        val completions = getCompletions(testCode).map { it.text }
        val expectedTexts = listOf("toInt()", "toIntervalZeroBased()", "roundToInt()", "toUInt()")
        assertEquals(expectedTexts, completions)
    }

    @Test
    fun `rest endpoint should provide fully qualified name if same name is imported`() {
        val code = """
            import java.util.Random
            fun main() {
                val rnd = Random$CARET_MARKER
            }
        """.trimIndent()

        val completions = getCompletions(code)
        val ktRandom = completions.find { it.displayText == "Random (kotlin.random)" && it.hasOtherImports == true }
            ?: error("Expected to find \"kotlin.random.Random\" completion, but got $completions\"")

        assertAll(
            { assertEquals("kotlin.random.Random", ktRandom.text) },
            { assertNull(ktRandom.import) },
        )
    }

    private fun getCompletions(textWithCaret: String): List<Completion> {
        val (code, position) = extractCaret { textWithCaret }
        return retrieveCompletionsFromEndpoint(code, position)
    }

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