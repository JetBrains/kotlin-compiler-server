package lsp.ws

import CompletionTest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import completions.configuration.WebSocketConfiguration
import lsp.utils.KotlinLspComposeExtension
import model.Completion
import org.eclipse.lsp4j.Position
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.test.StepVerifier
import java.net.URI
import java.util.UUID
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [completions.CompletionsApplication::class]
)
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(KotlinLspComposeExtension::class)
class KotlinLspProxyWSTest : CompletionTest {

    @LocalServerPort
    private var port: Int = 0
    private val baseWsUrl: URI by lazy {
        UriComponentsBuilder.fromUriString("ws://localhost:$port${WebSocketConfiguration.WEBSOCKET_COMPLETIONS_PATH}")
            .build(true).toUri()
    }

    private val defaultTimeout = 30.seconds

    private val client: TestWSClient by lazy {
        val testClient = TestWSClient({ baseWsUrl }, ReactorNettyWebSocketClient())
        testClient.connect(defaultTimeout)
        testClient
    }

    override fun performCompletionChecks(
        code: String,
        line: Int,
        character: Int,
        expected: List<String>,
        isJs: Boolean
    ) {
        if (isJs) return // silently ignore JS completions for now
        val requestId = UUID.randomUUID().toString()
        val payload = buildCompletionRequest(
            fileName = "test$requestId.kt",
            code = code,
            caret = Position(line, character),
            requestId = requestId,
        )

        val completionsMono = client.sendAndWaitCompletions(payload, requestId, defaultTimeout)

        StepVerifier.create(completionsMono)
            .assertNext { received ->
                val labels = received.map { it.displayText }
                assertAll(expected.map { exp ->
                    { assertTrue(labels.any { it.contains(exp) }, "Expected completion $exp but got $labels") }
                })
            }
            .verifyComplete()
    }

    private fun buildCompletionRequest(
        fileName: String,
        code: String,
        caret: Position,
        requestId: String,
    ): Map<String, Any> {
        val completionRequest = mapOf("files" to listOf(mapOf("name" to fileName, "text" to code)))
        return mapOf(
            "requestId" to requestId,
            "completionRequest" to completionRequest,
            "line" to caret.line,
            "ch" to caret.character,
        )
    }

    @AfterAll
    fun cleanup() = client.close()
}

private class TestWSClient(
    private val uriProvider: () -> URI,
    private val client: ReactorNettyWebSocketClient,
    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule(),
) {
    private val receiveSink = Sinks.many().replay().limit<String>(256)
    private val outboundSink = Sinks.many().unicast().onBackpressureBuffer<String>()
    private var connected = false
    private var subscription: Disposable? = null

    fun connect(timeout: Duration) {
        if (connected) return
        val connectMomo = client.execute(uriProvider()) { session ->
            val inbound = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(receiveSink::tryEmitNext)
                .doOnError(receiveSink::tryEmitError)
                .then()

            val outbound = session.send(outboundSink.asFlux().map(session::textMessage))
            outbound.and(inbound)
        }

        subscription = connectMomo.subscribe()
        connected = true

        receiveSink.asFlux()
            .filter { isInit(it) || isError(it) }
            .next()
            .timeout(timeout.toJavaDuration())
            .block()
    }

    fun sendAndWaitCompletions(
        payload: Map<String, Any>,
        requestId: String,
        timeout: Duration,
    ): Mono<List<Completion>> {
        check(connected) { "Not connected" }
        val json = objectMapper.writeValueAsString(payload)
        outboundSink.tryEmitNext(json)

        return receiveSink.asFlux()
            .filter { hasRequestId(it, requestId) }
            .next()
            .map { extractCompletions(it) }
            .timeout(timeout.toJavaDuration())
    }

    fun close() {
        subscription?.dispose()
        connected = false
    }

    private fun extractCompletions(json: String): List<Completion> {
        val node: JsonNode = objectMapper.readTree(json)
        val completionsNode = node["completions"] ?: return emptyList()
        return objectMapper.readValue(completionsNode.toString())
    }

    private fun isInit(json: String): Boolean =
        runCatching { objectMapper.readTree(json) }
            .map { node -> node["sessionId"] != null }
            .getOrDefault(false)

    private fun isError(json: String): Boolean =
        runCatching { objectMapper.readTree(json) }
            .map { node -> node["message"] != null && node["completions"] == null }
            .getOrDefault(false)

    private fun hasRequestId(json: String, expected: String): Boolean =
        runCatching { objectMapper.readTree(json) }
            .map { node -> node["requestId"]?.asText() == expected }
            .getOrDefault(false)
}