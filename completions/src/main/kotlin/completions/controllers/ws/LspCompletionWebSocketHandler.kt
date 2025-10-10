@file:Suppress("ReactiveStreamsUnusedPublisher")

package completions.controllers.ws

import completions.model.Project
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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

@Component
class LspCompletionWebSocketHandler(
    private val lspProxy: KotlinLspProxy,
    private val lspCompletionProvider: LspCompletionProvider,
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

    private fun getCompletionRequests(session: WebSocketSession, sideSink: Sinks.Many<Response>): Flux<CompletionRequest> =
        session.receive()
            .map { it.payloadAsText }
            .onBackpressureDrop { dropped ->
                runCatching { objectReader.readValue<CompletionRequest>(dropped) }.onSuccess {
                    sideSink.tryEmitNext(Response.Discarded(it.requestId))
                }
            }
            .flatMap({ payload ->
                val req = runCatching { objectReader.readValue<CompletionRequest>(payload) }.getOrNull()
                if (req == null) {
                    sideSink.tryEmitNext(Response.Error("Failed to parse request: $payload"))
                    Mono.empty()
                } else {
                    Mono.just(req)
                }
            }, 1)

    private fun handleCompletionRequest(sessionId: String, requests: Flux<CompletionRequest>): Flux<Response> =
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

    companion object {
        private val objectReader = objectMapper.readerFor(CompletionRequest::class.java)
    }
}

sealed interface Response {
    val requestId: String?

    open class Error(val message: String, override val requestId: String? = null) : Response
    data class Init(val sessionId: String, override val requestId: String? = null) : Response
    data class Completions(val completions: List<Completion>, override val requestId: String? = null) : Response
    data class Discarded(override val requestId: String) : Error("discarded", requestId)

    fun toJson(): String = objectMapper.writeValueAsString(this)
}

private data class CompletionRequest(
    val requestId: String,
    val project: Project,
    val line: Int,
    val ch: Int,
)

private val objectMapper = ObjectMapper().apply { registerKotlinModule() }