package lsp

import completions.configuration.lsp.LspProperties
import completions.dto.api.CompletionRequest
import completions.dto.api.ProjectFile
import completions.lsp.KotlinLspProxy
import kotlinx.coroutines.runBlocking
import completions.lsp.StatefulKotlinLspProxy.getCompletionsForClient
import completions.lsp.StatefulKotlinLspProxy.onClientConnected
import lsp.utils.CARET_MARKER
import lsp.utils.KotlinLspComposeExtension
import lsp.utils.extractCaret
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [completions.CompletionsApplication::class]
)
@ExtendWith(KotlinLspComposeExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LspProxyTest(
    @Autowired private val lspProperties: LspProperties,
) {

    private lateinit var lspProxy: KotlinLspProxy

    @Test
    fun `lsp proxy should be available`() {
        assertTrue(lspProxy.isAvailable())
    }

    @Test
    fun `lsp proxy should provide stateless completions`() = runBlocking {
        val (code, caret) = extractCaret {
            """
                fun main() {
                    listOf(1, 2, 3).fil$CARET_MARKER
            """.trimIndent()
        }
        val project = buildCompletionRequest(code)
        val completions = lspProxy.getOneTimeCompletions(project, caret.line, caret.character)
        val expected = listOf("filter", "filterIndexed", "filterIsInstance", "filterNot")
        assertTrue(completions.map { it.label }.containsAll(expected))
    }

    @Test
    fun `lsp proxy should provide stateful completions`() = runBlocking {
        val testClientId = "test-client-id"
        val (initialCode, initialCaret) = extractCaret {
            """
                fun main() {
                    listOf(1, 2, 3).fil$CARET_MARKER
            """.trimIndent()
        }

        lspProxy.onClientConnected(testClientId)
        var completions = lspProxy.getCompletionsForClient(
            testClientId,
            buildCompletionRequest(initialCode),
            initialCaret.line,
            initialCaret.character
        )
        var expected = listOf("filter", "filterIndexed", "filterIsInstance", "filterNot")
        assertTrue(completions.map { it.label }.containsAll(expected))

        val (changedCode, changedCaret) = extractCaret {
            """
                fun main() {
                    listOf(1, 2, 3).filter { it > 2 }
                      .ma$CARET_MARKER
            """.trimIndent()
        }
        completions = lspProxy.getCompletionsForClient(
            testClientId,
            buildCompletionRequest(changedCode),
            changedCaret.line,
            changedCaret.character
        )
        expected = listOf("map", "mapIndexed", "mapIndexedNotNull", "mapNotNull")
        assertTrue(completions.map { it.label }.containsAll(expected))
    }

    @Test
    fun `lsp proxy should NOT provide completions for unrecognized user`() = runBlocking {
        val testClientId = "unrecognised-client-id"
        val (initialCode, initialCaret) = extractCaret {
            """
                fun main() {
                    listOf(1, 2, 3).fil$CARET_MARKER
            """.trimIndent()
        }

        val completions = lspProxy.getCompletionsForClient(
            testClientId,
            buildCompletionRequest(initialCode),
            initialCaret.line,
            initialCaret.character
        )
        assertTrue(completions.isEmpty())
    }

    @AfterEach
    fun cleanup() = lspProxy.closeAllProjects()

    private fun buildCompletionRequest(code: String): CompletionRequest =
        CompletionRequest(files = listOf(ProjectFile(text = code, name = "test.kt")))

    @BeforeAll
    fun setup() = runBlocking {
        lspProxy = KotlinLspProxy(
            LspProperties(
                host = System.getProperty("LSP_HOST"),
                port = System.getProperty("LSP_PORT").toInt(),
                reconnectionRetries = 10,
                kotlinVersion = lspProperties.kotlinVersion
            )
        )
        lspProxy.initializeClient()
    }
}