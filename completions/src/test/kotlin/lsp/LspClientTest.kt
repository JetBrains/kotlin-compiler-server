package lsp

import CompletionTest
import completions.lsp.LspCompletionParser.toCompletion
import lsp.utils.CARET_MARKER
import lsp.utils.KotlinLspComposeExtension
import lsp.utils.extractCaret
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import completions.lsp.client.KotlinLspClient
import completions.lsp.client.LspClient
import org.eclipse.lsp4j.Position
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import kotlin.test.assertTrue
import kotlin.text.contains

@ExtendWith(KotlinLspComposeExtension::class)
class LspClientTest : CompletionTest {

    @Test
    fun `LSP client should initialize correctly`() {
        assertTrue { isClientInitialized() }
    }

    @Test
    fun `LSP client should provide completions for libs declared in build file (kotlinx-coroutines)`() = runBlocking {
        val snippet =
            """
                fun main() {
                    runBlock$CARET_MARKER
                }
            """.trimIndent()
        checkCompletions(snippet, listOf("runBlocking"))
    }

    private fun checkCompletions(snippet: String, expectedLabels: List<String>) = runBlocking {
        val (code, position) = extractCaret { snippet }
        val uri = randomResourceUri
        client.openDocument(uri, code)
        val completions = client.getCompletion(uri, position).await()
        assertAll(
            { assertTrue { completions.isNotEmpty() } },
            { assertTrue(completions.map { it.label }.containsAll(expectedLabels)) },
        )
    }

    @AfterEach
    fun cleanup() = runBlocking {
        openedDocuments.forEach { client.closeDocument(it) }
    }

    override fun performCompletionChecks(
        code: String,
        line: Int,
        character: Int,
        expected: List<String>,
        isJs: Boolean
    ) = runBlocking {
        if (isJs) return@runBlocking
        val caret = Position(line, character)
        val uri = randomResourceUri
        client.openDocument(uri, code)
        val received = client.getCompletion(uri, caret).await()

        val labels = received.mapNotNull { it.toCompletion()?.displayText }
        assertAll(expected.map { exp ->
            { assertTrue(labels.any { it.contains(exp) }, "Expected completion $exp but got $labels") }
        })
    }

    companion object {
        private val WORKSPACE_PATH = System.getProperty("LSP_REMOTE_WORKSPACE_ROOT") ?:
            LspClientTest::class.java.getResource("/lsp/lsp-users-projects-root-test")?.path
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
