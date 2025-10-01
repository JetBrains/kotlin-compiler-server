package com.compiler.server.controllers

import com.compiler.server.model.Project
import com.compiler.server.service.KotlinProjectExecutor
import com.compiler.server.service.lsp.KotlinLspProxy
import com.compiler.server.service.lsp.StatefulKotlinLspProxy.onClientConnected
import com.compiler.server.service.lsp.StatefulKotlinLspProxy.onClientDisconnected
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.getOrElse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import model.Completion
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class LspCompletionWebSocketHandler(
    private val lspProxy: KotlinLspProxy,
    private val kotlinProjectExecutor: KotlinProjectExecutor,
) : TextWebSocketHandler() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(
        Dispatchers.IO + job + CoroutineName("LspCompletionWebSocketHandler")
    )
    private val logger = LoggerFactory.getLogger(LspCompletionWebSocketHandler::class.java)

    private val completionJobs = ConcurrentHashMap<String, Job>()
    private val incomingCompletionsRequests = ConcurrentHashMap<String, Channel<CompletionRequest>>()

    private val responseJobs = ConcurrentHashMap<String, Job>()
    private val outgoingResponsesFlows = ConcurrentHashMap<String, MutableSharedFlow<Response>>()

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val request = session.decodeCompletionRequestFromTextMessage(message) ?: return
        incomingCompletionsRequests[session.id]?.trySend(request)
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val incoming = Channel<CompletionRequest>(capacity = Channel.UNLIMITED).also {
            incomingCompletionsRequests[session.id] = it
        }

        val responseFlow = MutableSharedFlow<Response>(
            extraBufferCapacity = 32,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        ).also { outgoingResponsesFlows[session.id] = it }

        val responseJob = scope.launch {
            try {
                responseFlow.collect { response ->
                    if (session.isOpen) session.sendMessage(TextMessage(response.toJson()))
                }
            } catch (t: Throwable) {
                if (t !is CancellationException) {
                    logger.warn("Error collecting responses for client ${session.id}: ${t.message}")
                }
            }
        }
        responseJobs[session.id] = responseJob

        val completionWorker = scope.launch {
            with(session) {
                logger.info("Lsp client connected: $id")
                runCatching {
                    lspProxy.requireAvailable()
                }.onFailure {
                    sendResponse(Response.Error("Proxy is still not connected to Language server"))
                    return@launch
                }
                lspProxy.onClientConnected(id)
                sendResponse(Response.Init(id))
            }

            incoming.processIncomingRequests(session)
        }
        completionWorker.invokeOnCompletion { completionJobs.remove(session.id) }
        completionJobs[session.id] = completionWorker
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        handleClientDisconnected(session.id)
        session.close(CloseStatus.NORMAL)
        logger.info("Lsp client disconnected: ${session.id} ($status)")
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        handleClientDisconnected(session.id)
        session.close(CloseStatus.SERVER_ERROR)
        logger.info("Lsp client transport error: {} ({}, {})", session.id, exception.cause, exception.message)
    }

    private fun handleClientDisconnected(clientId: String) {
        completionJobs.remove(clientId)?.cancel()
        responseJobs.remove(clientId)?.cancel()
        incomingCompletionsRequests.remove(clientId)
        outgoingResponsesFlows.remove(clientId)
        scope.launch { lspProxy.onClientDisconnected(clientId) }
    }

    private suspend fun Channel<CompletionRequest>.processIncomingRequests(session: WebSocketSession) {
        while (true) {
            val first = receiveCatching().getOrElse { return }
            var req = first

            while (true) {
                val next = tryReceive().getOrNull() ?: break
                session.sendResponse(Response.Discarded(requestId = req.requestId))
                req = next
            }

            runCatching { lspProxy.requireAvailable() }
                .onFailure { throwable ->
                    session.sendResponse(
                        Response.Error(
                            message = "LSP not available: ${throwable.cause}, ${throwable.message}",
                            requestId = req.requestId,
                        ),
                    )
                    continue
                }

            try {
                val res = kotlinProjectExecutor.completeWithLsp(
                    clientId = session.id, project = req.project, line = req.line, character = req.ch
                )
                session.sendResponse(Response.Completions(completions = res, requestId = req.requestId))
            } catch (e: Exception) {
                logger.warn("Completion processing failed for client ${session.id}:", e)
                session.sendResponse(
                    Response.Error(
                        message = e.message ?: "Unknown error", requestId = req.requestId
                    )
                )
            }
        }
    }

    private fun WebSocketSession.sendResponse(response: Response) {
        outgoingResponsesFlows[id]?.tryEmit(response)
    }

    private fun WebSocketSession.decodeCompletionRequestFromTextMessage(message: TextMessage): CompletionRequest? =
        try {
            objectMapper.readValue(message.payload, CompletionRequest::class.java)
        } catch (e: JsonProcessingException) {
            logger.warn("Invalid JSON from client: ${message.payload}")
            scope.launch { sendResponse(Response.Error("Invalid JSON format for ${message.payload}: ${e.message}")) }
            null
        }

    @PreDestroy
    fun cleanup() {
        lspProxy.closeAllProjects()
        this.job.cancel()
    }

    companion object {
        internal val objectMapper = ObjectMapper().apply { registerKotlinModule() }
    }
}

internal sealed interface Response {
    val requestId: String?

    open class Error(val message: String, override val requestId: String? = null) : Response
    data class Init(val sessionId: String, override val requestId: String? = null) : Response
    data class Completions(val completions: List<Completion>, override val requestId: String? = null) : Response
    data class Discarded(override val requestId: String) : Error("discarded", requestId)

    fun toJson(): String = LspCompletionWebSocketHandler.objectMapper.writeValueAsString(this)
}

private data class CompletionRequest(
    val requestId: String,
    val project: Project,
    val line: Int,
    val ch: Int,
)
