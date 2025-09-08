package lsp

import AbstractCompletionTest
import lsp.utils.CARET_MARKER
import lsp.utils.KotlinLspComposeExtension
import lsp.utils.extractCaret
import completions.model.Project
import completions.model.ProjectFile
import model.Completion
import model.Icon
import org.eclipse.lsp4j.Position
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [completions.CompletionsApplication::class]
)
@ExtendWith(KotlinLspComposeExtension::class)
class LspCompletionProviderTest : AbstractCompletionTest {

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
        val expectedTexts = listOf("toInt()", "toIntervalZeroBased()", "toUInt()", "roundToInt()")
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

    override fun performCompletion(
        code: String,
        line: Int,
        character: Int,
        completions: List<String>,
        isJs: Boolean
    ) {
        val caret = Position(line, character)
        val completions = retrieveCompletionsFromEndpoint(code, caret).map { it.displayText }
        assertAll(executables = completions.map { exp ->
            { assertTrue(completions.any { it.contains(exp) }, "Expected completion $exp but got $completions") }
        })
    }

    private fun retrieveCompletionsFromEndpoint(code: String, position: Position): List<Completion>  {
        val project = Project(files = listOf(ProjectFile(text = code, name = "file.kt")))
        val url = "http://localhost:$port/api/complete/lsp?line=${position.line}&ch=${position.character}"
        return withTimeout {
            post()
                .uri(url)
                .bodyValue(project)
                .exchange()
                .expectStatus().isOk
                .expectBodyList(Completion::class.java)
                .returnResult()
                .responseBody
        } ?: emptyList()

    }

    private fun <T> withTimeout(
        duration: Duration = 2.minutes.toJavaDuration(),
        body: WebTestClient.() -> T?
    ): T? = with(webTestClient.mutate().responseTimeout(duration).build()) { body() }
}