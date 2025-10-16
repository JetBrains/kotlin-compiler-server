@file:Suppress("ReactiveStreamsUnusedPublisher")

package completions.controllers.ws

import completions.model.Project
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import completions.lsp.KotlinLspProxy
import completions.lsp.StatefulKotlinLspProxy.onClientConnected
import completions.lsp.StatefulKotlinLspProxy.onClientDisconnected
import completions.service.lsp.LspCompletionProvider
import kotlinx.coroutines.reactor.mono
import model.Completion
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers

/**
 * WebSocket handler for stateful LSP code-completion sessions.
 *
 * Protocol (wire-level):
 * - **Connection**: Server sends `Init{ sessionId }` immediately after the WebSocket is established. The client could persist
 *   this identifier for the lifetime of the connection and treat it as the LSP client id.
 *
 * - **Requests**: Client sends `CompletionRequest{ requestId, project, line, ch }` for each caret position to be completed.
 *   `requestId` must be unique per client session and is used exclusively for correlation.
 *
 * - **Responses**: For a successfully processed request: `Completions{ completions, requestId }` where `completions` are
 *   a list of [Completion].If a request is dropped due to backpressure: `Discarded{ requestId }`.This is a terminal outcome
 *   for that requestId. On failure: Error{ message, requestId? }. If requestId is present, the error pertains to that
 *   specific request.
 *
 * The server serializes processing and maintains "latest-wins" semantics under pressure:
 * - While one request is processing, newly arrived requests may be enqueued. If the inbound rate exceeds capacity,
 *   all but the most recent enqueued request are discarded; each discarded request results in `Discarded{ requestId }`.
 * - The in-flight request is never cancelled. It is allowed to complete and will produce either Completions or Error.
 *
 * The rationale for not cancelling the in-flight request is due to the condition in which, with a high request rate,
 * cancelling the in-flight task would cause repeated aborts and potential starvation, making it possible for the client
 * to never receive any completions at all.
 */
@Component
class LspCompletionWebSocketHandler(
    private val lspProxy: KotlinLspProxy,
    private val lspCompletionProvider: LspCompletionProvider,
    private val objectMapper: ObjectMapper,
) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(LspCompletionWebSocketHandler::class.java)

    override fun handle(session: WebSocketSession): Mono<Void?> {
        val sessionId = session.id

        val sideSink = Sinks.many().unicast().onBackpressureBuffer<Response>()
        val sideFlux = sideSink.asFlux()

        val completions = handleCompletionRequest(
            sessionId = sessionId,
            requests = getCompletionRequests(session, sideSink)
        )

        return session.startCompletionHandler(
            initial = getInitialMono(sessionId),
            completions,
            sideFlux,
        )
    }

    private fun getInitialMono(sessionId: String): Mono<Response> = mono {
        runCatching {
            lspProxy.requireAvailable()
            lspProxy.onClientConnected(sessionId)
            Response.Init(sessionId)
        }.getOrElse { Response.Error("LSP not available: ${it.message}") }
    }.subscribeOn(Schedulers.boundedElastic())

    private fun getCompletionRequests(session: WebSocketSession, sideSink: Sinks.Many<Response>): Flux<WebSocketCompletionRequest> =
        session.receive()
            .map { it.payloadAsText }
            .onBackpressureDrop { dropped ->
                runCatching { objectMapper.readValue<WebSocketCompletionRequest>(dropped) }.onSuccess {
                    sideSink.tryEmitNext(Response.Discarded(it.requestId))
                }
            }
            .flatMap({ payload ->
                val req = runCatching { objectMapper.readValue<WebSocketCompletionRequest>(payload) }.getOrNull()
                if (req == null) {
                    sideSink.tryEmitNext(Response.Error("Failed to parse request: $payload"))
                    Mono.empty()
                } else {
                    Mono.just(req)
                }
            }, 1)

    private fun handleCompletionRequest(sessionId: String, requests: Flux<WebSocketCompletionRequest>): Flux<Response> =
        requests
            .concatMap({ request ->
                mono {
                    lspProxy.requireAvailable()
                    lspCompletionProvider.complete(
                        clientId = sessionId,
                        project = request.project,
                        line = request.line,
                        ch = request.ch,
                        applyFuzzyRanking = true,
                    )
                }
                    .subscribeOn(Schedulers.boundedElastic())
                    .map<Response> { Response.Completions(it, request.requestId) }
                    .onErrorResume { e ->
                        logger.warn("Completion processing failed for client $sessionId:", e)
                        Mono.just<Response>(Response.Error(e.message ?: "Unknown error", request.requestId))
                    }
            }, 1)

    private fun WebSocketSession.startCompletionHandler(
        initial: Mono<Response>,
        vararg outboundFluxes: Flux<Response>,
    ): Mono<Void?> {
        val outbound: Flux<WebSocketMessage> =
            Flux.merge(outboundFluxes.toList())
                .startWith(initial)
                .map { textMessage(it.toJson()) }

        return send(outbound)
            .doOnError { logger.warn("WS session error for client $id: ${it.message}") }
            .doFinally {
                runCatching { lspProxy.onClientDisconnected(id) }
                runCatching { close() }
            }
    }

    private fun Response.toJson(): String = objectMapper.writeValueAsString(this)
}

sealed interface Response {
    val requestId: String?

    open class Error(val message: String, override val requestId: String? = null) : Response
    data class Init(val sessionId: String, override val requestId: String? = null) : Response
    data class Completions(val completions: List<Completion>, override val requestId: String? = null) : Response
    data class Discarded(override val requestId: String) : Error("discarded", requestId)
}

private data class WebSocketCompletionRequest(
    val requestId: String,
    val project: Project,
    val line: Int,
    val ch: Int,
)