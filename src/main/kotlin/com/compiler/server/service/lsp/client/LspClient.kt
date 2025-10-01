package com.compiler.server.service.lsp.client

import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionTriggerKind
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration

interface LspClient : AutoCloseable {
    fun initRequest(kotlinProjectRoot: String, projectName: String = "None"): CompletableFuture<Void>

    /**
     * Wait until the client is ready. If [timeout] is specified, this method will throw a [kotlinx.coroutines.TimeoutCancellationException]
     * if the client is not ready within the specified time.
     *
     * @param timeout the maximum time to wait for the client to be ready, or `null` to wait indefinitely
     * @throws kotlinx.coroutines.TimeoutCancellationException if the client is not ready within the specified time
     */
    suspend fun awaitReady(timeout: Duration? = null)

    fun isReady(): Boolean

    fun getCompletion(
        uri: String,
        position: Position,
        triggerKind: CompletionTriggerKind = CompletionTriggerKind.Invoked,
    ): CompletableFuture<List<CompletionItem>>

    /**
     * Completion request on `textDocument/completion`, as [getCompletion].
     * This method retries the request if it fails due to a [ResponseErrorException] with code
     * [ResponseErrorCode.RequestFailed] and when the failing reason is due to the document not
     * yet present in the language server (i.e. `didOpen` has not been called yet or the notification
     * has not yet arrived). This method helps in situations where the language server is busy processing
     * clients' requests and the completion request is received before the `didOpen` notification is processed.
     */
    suspend fun getCompletionsWithRetry(
        uri: String,
        position: Position,
        triggerKind: CompletionTriggerKind = CompletionTriggerKind.Invoked,
        maxRetries: Int = 3,
    ): List<CompletionItem>

    fun openDocument(uri: String, content: String, version: Int = 1, languageId: String = "kotlin")

    fun changeDocument(uri: String, newContent: String, version: Int = 1)

    fun closeDocument(uri: String)

    fun shutdown(): CompletableFuture<Any>

    fun exit()

    override fun close() = runBlocking {
        shutdown().await()
        exit()
    }

    companion object Companion {

        /**
         * Creates and initialize an LSP client.
         *
         * [kotlinProjectRoot] is the path ([[java.net.URI.path]]) to the root project directory,
         * where the project must be a project supported by [Kotlin-LSP](https://github.com/Kotlin/kotlin-lsp).
         * The workspace will not contain users' files, but it can be used to store common files,
         * to specify kotlin/java versions, project-wide imported libraries and so on.
         *
         * @param kotlinProjectRoot the path to the workspace directory, namely the root of the common project
         * @param projectName the name of the project
         */
        suspend fun createSingle(
            kotlinProjectRoot: String,
            projectName: String = "None",
            host: String = LspConnectionManager.lspHost,
            port: Int = LspConnectionManager.lspPort,
        ): KotlinLspClient {
            return KotlinLspClient(host, port).apply {
                initRequest(kotlinProjectRoot, projectName).await()
            }
        }
    }
}

interface ReconnectingLspClient : LspClient {
    fun addOnDisconnectListener(listener: () -> Unit)
    fun addOnReconnectListener(listener: () -> Unit)
}