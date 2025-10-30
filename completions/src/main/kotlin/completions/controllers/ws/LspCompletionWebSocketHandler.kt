@file:Suppress("ReactiveStreamsUnusedPublisher")

package completions.controllers.ws

import com.fasterxml.jackson.databind.ObjectMapper
import completions.configuration.lsp.LspProperties
import completions.dto.api.CompletionRequest
import completions.service.lsp.LspCompletionProvider
import kotlinx.coroutines.reactor.mono
import completions.dto.api.CompletionResponse
import completions.lsp.MultipleVersionsKotlinLspProxy
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
 * - **Requests**: Client sends `CompletionRequest{ requestId, completionRequest, line, ch }` for each caret position to be completed.
 *   `requestId` must be unique per client session and is used exclusively for correlation.
 *
 * - **Responses**: For a successfully processed request: `Completions{ completions, requestId }` where `completions` are
 *   a list of [CompletionResponse].If a request is dropped due to backpressure: `Discarded{ requestId }`.This is a terminal outcome
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
    private val lspProxy: MultipleVersionsKotlinLspProxy,
    private val lspCompletionProvider: LspCompletionProvider,
    private val objectMapper: ObjectMapper,
    private val lspProperties: LspProperties,
) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(LspCompletionWebSocketHandler::class.java)
    private val completionRequestReader = objectMapper.readerFor(WebSocketCompletionRequest::class.java)
    private val initRequestReader = objectMapper.readerFor(InitializationRequest::class.java)

    override fun handle(session: WebSocketSession): Mono<Void?> {
        val sessionId = session.id

        val sideSink = Sinks.many().unicast().onBackpressureBuffer<Response>()
        val sideFlux = sideSink.asFlux()

        val inbound = session.receive().map { it.payloadAsText }.share()

        val initResponses = handleInitializationRequests(sessionId, inbound)

        val completions = handleCompletionRequest(sessionId, inbound, sideSink)

        return session.startCompletionHandler(
            initial = getInitialMono(sessionId),
            initResponses,
            completions,
            sideFlux,
        )
    }

    private fun handleInitializationRequests(sessionId: String, inbound: Flux<String>): Flux<Response> =
        inbound
            .flatMap({ payload ->
                mono {
                    runCatching { initRequestReader.readValue<InitializationRequest>(payload) }
                        .getOrNull()
                }.subscribeOn(Schedulers.boundedElastic())
            }, 1)
            .filter { it != null }
            .concatMap({ init ->
                mono {
                    runCatching {
                        lspProxy.requireAvailable(init.kotlinVersion)
                        lspProxy.onClientConnected(sessionId, init.kotlinVersion)
                        Response.Initialized(sessionId, init.kotlinVersion, init.requestId)
                    }.getOrElse {
                        Response.Error("Initialization failed: ${it.message}", init.requestId)
                    }
                }.subscribeOn(Schedulers.boundedElastic())
            }, 1)

    private fun getInitialMono(sessionId: String): Mono<Response> = mono {
        runCatching {
            lspProxy.requireAvailable()
            Response.Init(sessionId)
        }.getOrElse { Response.Error("LSP not available: ${it.message}") }
    }.subscribeOn(Schedulers.boundedElastic())

    private fun handleCompletionRequest(
        sessionId: String,
        inbound: Flux<String>,
        sideSink: Sinks.Many<Response>,
    ): Flux<Response> =
        inbound
            .onBackpressureDrop { dropped ->
                runCatching { completionRequestReader.readValue<WebSocketCompletionRequest>(dropped) }.onSuccess {
                    sideSink.tryEmitNext(Response.Discarded(it.requestId))
                }
            }
            .flatMap({ payload ->
                runCatching { completionRequestReader.readValue<WebSocketCompletionRequest>(payload) }
                    .fold(
                        onSuccess = { Mono.just(it) },
                        onFailure = { Mono.empty() }
                    )
            }, 1)
            .concatMap({ request ->
                mono {
                    val version = request.kotlinVersion ?: lspProperties.kotlinVersion
                    lspProxy.requireAvailable(version)
                    lspCompletionProvider.complete(
                        clientId = sessionId,
                        request = request.completionRequest,
                        line = request.line,
                        ch = request.ch,
                        kotlinVersion = version,
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
                runCatching { mono { lspProxy.onClientDisconnected(id) } }
                runCatching { close() }
            }
    }

    private fun Response.toJson(): String = objectMapper.writeValueAsString(this)
}

sealed interface Response {
    val requestId: String?

    open class Error(@Suppress("unused") val message: String, override val requestId: String? = null) : Response
    data class Init(val sessionId: String, override val requestId: String? = null) : Response
    data class Initialized(val clientId: String, val kotlinVersion: String, override val requestId: String?) : Response
    data class Completions(val completions: List<CompletionResponse>, override val requestId: String? = null) : Response
    data class Discarded(override val requestId: String) : Error("discarded", requestId)
}

private data class WebSocketCompletionRequest(
    val requestId: String,
    val completionRequest: CompletionRequest,
    val line: Int,
    val ch: Int,
    val kotlinVersion: String? = null,
)

private data class InitializationRequest(
    val requestId: String,
    val kotlinVersion: String,
)