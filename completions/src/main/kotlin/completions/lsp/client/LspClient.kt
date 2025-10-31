package completions.lsp.client

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

    /**
     * Opens a document in the language server. This operation notifies the server that a document
     * corresponding to the provided URI has been opened with the specified content, version,
     * and language identifier.
     *
     * > Please note that from LSP specification perspective, this operation is a `Notification`,
     * meaning that it is not expected to return a response.
     *
     * @param uri The unique identifier for the document to open.
     * @param content The initial content of the document being opened.
     * @param version The version of the document being opened, must be monotonically increasing.
     * @param languageId The identifier for the language associated with the document.
     */
    fun openDocument(uri: String, content: String, version: Int = 1, languageId: String = "kotlin")

    /**
     * Changes the content of a document in the language server. This operation notifies the server
     * that the content of a document corresponding to the provided URI has been changed with the
     * specified content, version, and language identifier. As per current implementation,
     * incremental changes are not supported, so whole file content is transmitted.
     *
     * > Please note that from LSP specification perspective, this operation is a `Notification`,
     * meaning that it is not expected to return a response.
     *
     * @param uri The unique identifier for the document to change.
     * @param newContent The new content of the opened document.
     * @param version The new version of the opened document.
     */
    fun changeDocument(uri: String, newContent: String, version: Int = 1)

    /**
     * Closes a document in the language server.
     *
     * > Please note that from LSP specification perspective, this operation is a `Notification`,
     * meaning that it is not expected to return a response.
     *
     * @param uri The unique identifier for the document to close.
     */
    fun closeDocument(uri: String)

    /**
     * Requests the shutdown of the language server client. This method sends a shutdown request
     * to the language server, signaling that the client intends to terminate its connection.
     * The server is expected to respond, finalizing any necessary cleanup before the client disconnects.
     *
     * @return A [CompletableFuture] that completes when the shutdown request is acknowledged
     *         by the server or fails if an error occurs during the process.
     */
    fun shutdown(): CompletableFuture<Any>

    /**
     * Sends an exit notification to the language server, indicating that the client has terminated.
     * This operation is expected to be the final communication sent to the server after a shutdown request.
     *
     * The exit notification serves to inform the language server that no further requests or notifications
     * will be made by the client, allowing the server to perform any necessary cleanup or shut down operations.
     *
     * > From the LSP specification perspective, this operation is a `Notification`, meaning that it does not
     * expect a response from the server.
     */
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
            host: String,
            port: Int,
        ): KotlinLspClient {
            return KotlinLspClient(host, port).apply {
                initRequest(kotlinProjectRoot, projectName).await()
            }
        }
    }
}

/**
 * Represents a client that extends the capabilities of [LspClient] by providing
 * behavior to handle reconnection scenarios and managing event listeners for
 * connection state changes.
 */
interface ReconnectingLspClient : LspClient {
    fun addOnDisconnectListener(listener: () -> Unit)
    fun addOnReconnectListener(listener: () -> Unit)
}