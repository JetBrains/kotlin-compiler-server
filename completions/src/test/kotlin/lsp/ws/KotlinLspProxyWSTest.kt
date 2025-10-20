package lsp.ws

import CompletionTest
import ImportTest
import completions.configuration.WebSocketConfiguration
import lsp.utils.KotlinLspComposeExtension
import completions.dto.api.Completion
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import lsp.utils.TestWSClient
import lsp.utils.TestWSClient.Companion.buildCompletionRequest
import lsp.utils.extractCaret
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.URI
import java.util.UUID
import kotlin.test.Ignore
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [completions.CompletionsApplication::class]
)
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(KotlinLspComposeExtension::class)
class KotlinLspProxyWSTest : CompletionTest, ImportTest {

    @LocalServerPort
    private var port: Int = 0
    private val baseWsUrl: URI by lazy {
        UriComponentsBuilder.fromUriString("ws://localhost:$port${WebSocketConfiguration.WEBSOCKET_COMPLETIONS_PATH}")
            .build(true).toUri()
    }

    private val defaultTimeout = 30.seconds

    private lateinit var client: TestWSClient

    @BeforeAll
    fun setup() {
        client = TestWSClient({ baseWsUrl }, ReactorNettyWebSocketClient())
        client.connect()
    }

    override fun performCompletionChecks(
        codeWithCaret: String,
        expected: List<String>,
        isJs: Boolean
    ) {
        assumeFalse(isJs, "JS completions are not supported by LSP yet.")
        val completionsMono = getCompletionMono(codeWithCaret)

        StepVerifier.create(completionsMono)
            .assertNext { received ->
                val labels = received.map { it.displayText }
                assertAll(expected.map { exp ->
                    { assertTrue(labels.any { it.contains(exp) }, "Expected completion $exp but got $labels") }
                })
            }
            .verifyComplete()
    }

    override fun getCompletions(
        codeWithCaret: String,
        isJs: Boolean
    ): List<Completion> = runBlocking {
        assumeFalse(isJs, "JS completions are not supported by LSP yet.")
        getCompletionMono(codeWithCaret).awaitSingle()
    }

    @Ignore("(IJPL-213504) Auto-completion/auto-import issue with external library")
    @Test
    override fun `brackets after import completion`() { }

    private fun getCompletionMono(codeWithCaret: String): Mono<List<Completion>> {
        val requestId = UUID.randomUUID().toString()
        val (code, caret) = extractCaret { codeWithCaret }
        val payload = buildCompletionRequest(
            fileName = "test$requestId.kt",
            code = code,
            caret = caret,
            requestId = requestId,
        )
        return client.sendAndWaitCompletions(payload, requestId, defaultTimeout)
    }

    @AfterAll
    fun cleanup() = client.close()
}
