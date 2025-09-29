package com.compiler.server.lsp.ws

import com.compiler.server.AbstractCompletionTest
import com.compiler.server.lsp.utils.KotlinLspComposeExtension
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.websocket.ContainerProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import kotlin.time.Duration.Companion.seconds

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(KotlinLspComposeExtension::class)
class KotlinLspProxyWSTest: AbstractCompletionTest {

    @LocalServerPort
    private var port: Int = 0

    private val url
        get() = "ws://localhost:$port/lsp/complete"

    override fun performCompletion(
        code: String,
        line: Int,
        character: Int,
        completions: List<String>,
        isJs: Boolean
    ) {
        if (isJs) return
        checkCompletionsWithWebSocketSession(code, line, character, completions)
    }

    fun checkCompletionsWithWebSocketSession(
        content: String,
        line: Int,
        ch: Int,
        expectedCompletions: List<String>
    ) = runBlocking {
        val session = connect()
        val msg = buildCompletionRequest(session.id, content, line, ch)
        val requestId = msg["requestId"] as String
        session.sendMessage(TextMessage(objectMapper.writeValueAsString(msg)))
        val completions = handler.receiveCompletions(requestId)
        val asserts = expectedCompletions.map { expected ->
            {
                assertTrue(completions.any { received ->
                    received.contains(expected)
                }, "Expected to find $expected in $completions")
            }
        }
        assertAll(asserts)
    }

    private suspend fun connect(): WebSocketSession {
        val session = withTimeoutOrNull(defaultTimeout) {
            client.execute(handler, url).await()
        } ?: error("Failed to connect to LSP server at $url within $defaultTimeout")
        val initMessage = handler.receiveMessage()

        assertNotNull(objectMapper.readTree(initMessage))
        return session
    }

    private fun buildCompletionRequest(sessionId: String, content: String, line: Int, ch: Int): Map<String, Any> {
        val project = mapOf(
            "files" to listOf(mapOf("name" to "$sessionId.kt", "text" to content)),
            "confType" to "java",
        )
        return mapOf(
            "requestId" to java.util.UUID.randomUUID().toString(),
            "project" to project,
            "line" to line,
            "ch" to ch,
        )
    }

    companion object {
        private val defaultTimeout = 20.seconds
        private lateinit var client: WebSocketClient
        private lateinit var handler: TestClientHandler
        private val objectMapper = ObjectMapper().apply { registerKotlinModule() }

        @BeforeAll
        @JvmStatic
        fun setup() {
            client = StandardWebSocketClient(
                ContainerProvider.getWebSocketContainer().apply {
                    defaultMaxSessionIdleTimeout = 0L
                    defaultMaxTextMessageBufferSize = 1_000_000
                    defaultMaxBinaryMessageBufferSize = 1_000_000
                }
            )
            handler = TestClientHandler()
        }

        private class TestClientHandler() : TextWebSocketHandler() {
            private val messages: Channel<String> = Channel(Channel.UNLIMITED)

            override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                messages.trySend(message.payload)
            }

            suspend fun receiveMessage(): String =
                withTimeout(defaultTimeout) {
                    messages.receive()
                }

            suspend fun receiveCompletions(expectedRequestId: String): List<String> {
                val msg = receiveMessage()
                val json = objectMapper.readTree(msg)
                return extractCompletionTexts(json, expectedRequestId)
            }

            private fun extractCompletionTexts(msg: JsonNode, expectedRequestId: String): List<String> {
                msg["requestId"]?.asText()?.takeIf { it == expectedRequestId }
                    ?: error("Invalid requestId, expected: $expectedRequestId, actual: ${msg["requestId"]?.asText()}")
                val completions = msg["completions"] ?: return emptyList()
                return completions.mapNotNull { it["displayText"]?.asText() }
            }
        }
    }
}