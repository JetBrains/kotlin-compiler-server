package lsp.ws

import ConcurrencyCompletionRunnerTest
import completions.configuration.WebSocketConfiguration
import completions.dto.api.CompletionResponse
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import lsp.utils.KotlinLspComposeExtension
import lsp.utils.TestWSClient
import lsp.utils.TestWSClient.Companion.buildCompletionRequest
import lsp.utils.extractCaret
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.UUID
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.use

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [completions.CompletionsApplication::class]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(KotlinLspComposeExtension::class)
class StatefulConcurrencyCompletionRunnerTest : ConcurrencyCompletionRunnerTest() {

    @LocalServerPort
    private var port: Int = 0
    private val baseWsUrl: URI by lazy {
        UriComponentsBuilder.fromUriString("ws://localhost:$port${WebSocketConfiguration.WEBSOCKET_COMPLETIONS_PATH}")
            .build(true).toUri()
    }

    private val defaultTimeout = 30.seconds

    @BeforeAll
    fun setup() {
        createAndConnectClient().use { _ -> /* warmup */ }
    }

    override fun performCompletionChecks(
        codeWithCaret: String,
        expected: List<String>,
        isJs: Boolean
    ) = runBlocking {
        assumeFalse(isJs, "JS completions are not supported by LSP yet.")
        createAndConnectClient().use { client ->
            val completions = getCompletions(client, codeWithCaret)
            val labels = completions.map { it.displayText }

            expected.forEach { exp ->
                assertTrue(
                    labels.any { it.contains(exp) },
                    "Expected completion '$exp' but got $labels"
                )
            }
        }
    }

    private fun createAndConnectClient(): TestWSClient =
        TestWSClient({ baseWsUrl }, ReactorNettyWebSocketClient()).also { it.connect() }

    private suspend fun getCompletions(
        client: TestWSClient,
        codeWithCaret: String
    ): List<CompletionResponse> {
        val requestId = UUID.randomUUID().toString()
        val (code, caret) = extractCaret { codeWithCaret }
        val payload = buildCompletionRequest(
            fileName = "test$requestId.kt",
            code = code,
            caret = caret,
            requestId = requestId,
        )
        return client.sendAndWaitCompletions(payload, requestId, defaultTimeout).awaitSingle()
    }
}
