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
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import model.Completion
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Component
class LspCompletionWebSocketHandler(
    private val lspProxy: KotlinLspProxy,
    private val kotlinProjectExecutor: KotlinProjectExecutor,
): TextWebSocketHandler() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(
        Dispatchers.IO + job + CoroutineName("LspCompletionWebSocketHandler")
    )

    private val activeSession = ConcurrentHashMap<String, WebSocketSession>()
    private val logger = LoggerFactory.getLogger(LspCompletionWebSocketHandler::class.java)

    private val sessionLocks = ConcurrentHashMap<String, Mutex>()
    private val completionsJob = ConcurrentHashMap<String, Job>()
    private val sessionFlows = ConcurrentHashMap<String, MutableSharedFlow<CompletionRequest>>()

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val request = session.decodeCompletionRequestFromTextMessage(message) ?: return
        sessionFlows[session.id]?.tryEmit(request)
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        activeSession[session.id] = session

        val flow =  MutableSharedFlow<CompletionRequest>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        ).also { sessionFlows[session.id] = it }

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

            flow
                .collectLatest { req ->
                    val available = withTimeoutOrNull(LSP_TIMEOUT_WAIT_TIME) {
                        while (!lspProxy.isAvailable()) {
                            delay(LSP_TIMEOUT_POLL_INTERVAL)
                        }
                        true
                    } ?: false

                    if (!available) {
                        session.sendResponse(Response.Error(message = "LSP not available", req.requestId))
                        return@collectLatest
                    }

                    try {
                        val res = kotlinProjectExecutor.completeWithLsp(
                            clientId = session.id,
                            project = req.project,
                            line = req.line,
                            character = req.ch
                        )
                        session.sendResponse(Response.Completions(completions = res, requestId = req.requestId))
                    } catch (e: Exception) {
                        logger.warn("Completion processing failed for client ${session.id}:", e)
                        session.sendResponse(
                            Response.Error(
                                message = "Completion failed: ${e.message}",
                                requestId = req.requestId
                            )
                        )
                    }
                }
        }
        completionWorker.invokeOnCompletion { completionsJob.remove(session.id) }
        completionsJob[session.id] = completionWorker
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        handleClientDisconnected(session.id)
        session.close(CloseStatus.NORMAL)
        logger.info("Lsp client disconnected: ${session.id} ($status)")
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        handleClientDisconnected(session.id)
        session.close(CloseStatus.SERVER_ERROR)
        logger.error("Lsp client transport error: ${session.id} (${exception.message})")
    }

    private fun handleClientDisconnected(clientId: String) {
        activeSession.remove(clientId)
        sessionFlows.remove(clientId)
        completionsJob.remove(clientId)?.cancel()
        scope.launch { lspProxy.onClientDisconnected(clientId) }
    }

    private suspend fun WebSocketSession.sendResponse(response: Response) {
        val mutex = sessionLocks.computeIfAbsent(id) { Mutex() }
        try {
            mutex.withLock {
                if (isOpen) sendMessage(TextMessage(response.toJson()))
            }
        } catch (e: Exception) {
            logger.warn("Error sending message to client $id:", e.message)
        }
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
        private val LSP_TIMEOUT_WAIT_TIME = 10.seconds
        private val LSP_TIMEOUT_POLL_INTERVAL = 100.milliseconds
    }
}

internal sealed interface Response {
    val requestId: String?

    data class Error(val message: String, override val requestId: String? = null) : Response
    data class Init(val sessionId: String, override val requestId: String? = null) : Response
    data class Completions(val completions: List<Completion>, override val requestId: String? = null) : Response

    fun toJson(): String = LspCompletionWebSocketHandler.objectMapper.writeValueAsString(this)
}

private data class CompletionRequest(
    val requestId: String,
    val project: Project,
    val line: Int,
    val ch: Int,
)
