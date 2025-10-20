package lsp.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import completions.dto.api.Completion
import org.eclipse.lsp4j.Position
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

internal class TestWSClient(
    private val uriProvider: () -> URI,
    private val client: ReactorNettyWebSocketClient,
    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule(),
) : AutoCloseable {
    private val receiveSink = Sinks.many().replay().limit<String>(256)
    private val outboundSink = Sinks.many().unicast().onBackpressureBuffer<String>()
    private var connected = false
    private var subscription: Disposable? = null

    fun connect(timeout: Duration = 2.minutes) {
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

    override fun close() {
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

    companion object {

        fun buildCompletionRequest(
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
    }
}