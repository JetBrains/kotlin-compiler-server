package lsp

import CompletionTest
import completions.lsp.LspCompletionParser
import lsp.utils.CARET_MARKER
import lsp.utils.extractCaret
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import completions.lsp.client.KotlinLspClient
import completions.lsp.client.LspClient
import lsp.utils.LspIntegrationTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID
import kotlin.test.assertTrue
import kotlin.text.contains

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [completions.CompletionsApplication::class]
)
class LspClientTest : CompletionTest, LspIntegrationTest() {

    @Autowired
    private lateinit var lspCompletionParser: LspCompletionParser

    @Test
    fun `LSP client should initialize correctly`() {
        assertTrue { isClientInitialized() }
    }

    @Test
    fun `LSP client should provide completions for libs declared in build file (kotlinx-coroutines)`() = runBlocking {
        val snippet =
            """
                import kotlinx.coroutines.runBlocking
                fun main() {
                    runBlock$CARET_MARKER
                }
            """.trimIndent()
        performCompletionChecks(snippet, listOf("runBlocking"))
    }

    @AfterEach
    fun cleanup() = runBlocking {
        openedDocuments.forEach { client.closeDocument(it) }
    }

    override fun performCompletionChecks(
        codeWithCaret: String,
        expected: List<String>,
        isJs: Boolean
    ) = runBlocking {
        assumeFalse(isJs, "JS completions are not supported by LSP yet.")
        val (code, caret) = extractCaret { codeWithCaret }
        val uri = randomResourceUri
        client.openDocument(uri, code)
        val received = client.getCompletion(uri, caret).await()

        val labels = received.mapNotNull { lspCompletionParser.toCompletion(it)?.displayText }
        assertAll(expected.map { exp ->
            { assertTrue(labels.any { it.contains(exp) }, "Expected completion $exp but got $labels") }
        })
    }

    companion object {
        private val WORKSPACE_PATH = System.getProperty("LSP_REMOTE_WORKSPACE_ROOT")
            ?: LspClientTest::class.java.getResource("/lsp/workspaces/lsp-users-projects-root")?.path
            ?: error("Could not find LSP remote workspace root")
        private val LSP_HOST = System.getProperty("LSP_HOST") ?: "localhost"
        private val LSP_PORT = System.getProperty("LSP_PORT")?.toInt() ?: 9999

        private const val WORKSPACE_NAME = "test"

        private val openedDocuments = mutableListOf<String>()
        private val randomResourceUri
            get() = "file:///lspClientTest/${UUID.randomUUID()}.kt".also { openedDocuments.add(it) }

        private lateinit var client: KotlinLspClient

        fun isClientInitialized() = ::client.isInitialized

        @BeforeAll
        @JvmStatic
        fun setup() = runBlocking {
            if (isClientInitialized()) {
                client.close()
            }
            client = LspClient.createSingle(WORKSPACE_PATH, WORKSPACE_NAME, host = LSP_HOST, port = LSP_PORT)
        }

        @AfterAll
        @JvmStatic
        fun teardown() = runBlocking {
            if (::client.isInitialized) {
                client.shutdown().await()
                client.exit()
            }
        }
    }
}
