package lsp

import lsp.utils.CARET_MARKER
import lsp.utils.KotlinLspComposeExtension
import lsp.utils.extractCaret
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import completions.lsp.client.KotlinLspClient
import completions.lsp.client.LspClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@ExtendWith(KotlinLspComposeExtension::class)
class LspClientTest {

    @Test
    fun `LSP client should initialize correctly`() {
        assertTrue { isClientInitialized() }
    }

    @Test
    fun `LSP client should provide completions for local variables`() = runBlocking {
        val (code, position) = extractCaret {
            """
                fun main() {
                    val alex = 1
                    val alex1 = 1 + a$CARET_MARKER
                }
            """.trimIndent()
        }

        client.openDocument(FAKE_RESOURCE_URI, code)
        delay(1.seconds)
        val completions = client.getCompletion(FAKE_RESOURCE_URI, position).await()
        assertAll(
            { assertTrue { completions.isNotEmpty() } },
            { assertContains(completions.map { it.label }, "alex") },
            { assertEquals("Int", completions.first { it.label == "alex" }.labelDetails?.description) }
        )
    }

    @Test
    fun `LSP client should provide completions for stdlib elements`() = runBlocking {
        val (code, position) = extractCaret {
            """
                fun main() {
                    3.0.toIn$CARET_MARKER
                }
            """.trimIndent()
        }
        client.openDocument(FAKE_RESOURCE_URI, code)
        delay(1.seconds)
        val completions = client.getCompletion(FAKE_RESOURCE_URI, position).await()
        assertAll(
            { assertTrue { completions.isNotEmpty() } },
            { assertContains(completions.map { it.label }, "toInt") },
        )
    }

    @Test
    fun `LSP client should provide completions for libs declared in build file (kotlinx-coroutines)`() = runBlocking {
        val (code, position) = extractCaret {
            """
                fun main() {
                    runBlock$CARET_MARKER
                }
            """.trimIndent()
        }
        client.openDocument(FAKE_RESOURCE_URI, code)
        delay(1.seconds)
        val completions = client.getCompletion(FAKE_RESOURCE_URI, position).await()
        assertAll(
            { assertTrue { completions.isNotEmpty() } },
            { assertContains(completions.map { it.label }, "runBlocking") },
        )
    }

    @AfterEach
    fun cleanup() = runBlocking {
        client.closeDocument(FAKE_RESOURCE_URI)
    }

    companion object {
        private val WORKSPACE_PATH = System.getProperty("LSP_USERS_PROJECTS_ROOT") ?: "/lsp-users-projects-root"
        private const val WORKSPACE_NAME = "test"
        private const val FAKE_RESOURCE_URI = "file:///foo/bar/File.kt"

        private lateinit var client: KotlinLspClient

        fun isClientInitialized() = ::client.isInitialized

        @BeforeAll
        @JvmStatic
        fun setup() = runBlocking {
            if (isClientInitialized()) {
                client.close()
            }
            client = LspClient.createSingle(WORKSPACE_PATH, WORKSPACE_NAME)
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
