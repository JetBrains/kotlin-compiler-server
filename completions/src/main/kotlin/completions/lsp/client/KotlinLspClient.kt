package completions.lsp.client

import completions.lsp.client.LspConnectionManager.Companion.exponentialBackoffMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import org.eclipse.lsp4j.services.LanguageServer
import org.slf4j.LoggerFactory
import java.io.EOFException
import java.net.SocketException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class KotlinLspClient(
    host: String = LspConnectionManager.lspHost(),
    port: Int = LspConnectionManager.lspPort(),
) : ReconnectingLspClient {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val connectionManager = LspConnectionManager(host, port)

    private val disconnectListeners = CopyOnWriteArrayList<() -> Unit>()
    private val reconnectListeners = CopyOnWriteArrayList<() -> Unit>()

    private val languageServer: LanguageServer
        get() = connectionManager.ensureConnected()

    private lateinit var initParams: InitializeParams

    private val readyState = ReadyState()

    init {
        readyState.reset()

        connectionManager.addOnDisconnectListener {
            logger.warn("Lost connection to LSP server, reconnecting...")
            readyState.reset()
            disconnectListeners.forEach { runCatching { it() } }
        }

        connectionManager.addOnReconnectListener {
            logger.debug("Reconnected to LSP server, re-initializing client...")
            if (::initParams.isInitialized) {
                runCatching {
                    languageServer.initialize(initParams).join()
                    languageServer.initialized(InitializedParams())
                    logger.debug("LSP client re-initialized")
                    readyState.complete()
                }.onFailure {
                    logger.warn("Re-initialize LSP client failed: ${it.message}")
                    readyState.fail(it)
                }
            }
            reconnectListeners.forEach { runCatching { it() } }
        }
        connectionManager.ensureConnected(initial = true)
    }

    override fun initRequest(kotlinProjectRoot: String, projectName: String): CompletableFuture<Void> {
        val capabilities = getCompletionCapabilities()
        val workspaceFolders = listOf(WorkspaceFolder("file://$kotlinProjectRoot", projectName))

        val params = InitializeParams().apply {
            this.capabilities = capabilities
            this.workspaceFolders = workspaceFolders
        }

        logger.debug("Initializing LSP client...")

        return languageServer.initialize(params)
            .thenCompose { res ->
                logger.debug(">>> Initialization response from server:\n{}", res)
                languageServer.initialized(InitializedParams())
                logger.info("LSP client initialized with workspace={}, name={}", kotlinProjectRoot, projectName)
                initParams = params
                readyState.complete()
                CompletableFuture.completedFuture(null)
            }
    }

    override fun getCompletion(
        uri: String,
        position: Position,
        triggerKind: CompletionTriggerKind,
    ): CompletableFuture<List<CompletionItem>> {
        val context = CompletionContext(triggerKind)
        val params = CompletionParams(
            TextDocumentIdentifier(uri),
            position,
            context
        )

        return languageServer.textDocumentService.completion(params)
            .thenCompose { res ->
                when {
                    res.isLeft -> res.left
                    res.isRight -> res.right.items
                    else -> emptyList()
                }.let { CompletableFuture.completedFuture(it) }
            }
    }

    override suspend fun getCompletionsWithRetry(
        uri: String,
        position: Position,
        triggerKind: CompletionTriggerKind,
        maxRetries: Int,
    ): List<CompletionItem> {
        var attempt = 0
        runCatching { awaitReady(RECONNECT_TIMEOUT.seconds) }.onFailure {
            logger.debug("LSP not ready")
            return emptyList()
        }
        do {
            try {
                return withTimeout(COMPLETIONS_TIMEOUT.seconds) {
                    getCompletion(uri, position, triggerKind).await()
                }
            } catch (e: Throwable) {
                when (e) {
                    is CompletionException, is ResponseErrorException -> {
                        if ((e as? ResponseErrorException)?.responseError?.code != ResponseErrorCode.RequestFailed.value) {
                            break
                        }
                        if (e.message?.contains("Document with url FileUrl(url='$uri'") ?: false) {
                            logger.info("Failed to get completions (document not ready), retrying... (${attempt + 1}/$maxRetries)")
                            delay(exponentialBackoffMillis(attempt = attempt).milliseconds)
                            attempt++
                            continue
                        }

                        if (e.cause is SocketException || e.cause is EOFException) { // mid-flight fail
                            logger.info("Completion failed due to connection issue, awaiting reconnection")
                            runCatching { awaitReady(RECONNECT_TIMEOUT.seconds) }
                                .onFailure { return emptyList() }
                            attempt++
                            continue
                        }
                    }
                    is CancellationException -> {
                        return emptyList()
                    }
                }
                logger.warn("Failed to get completions", e)
                return emptyList()
            }
        } while (attempt < maxRetries)
        return emptyList()
    }

    override fun openDocument(uri: String, content: String, version: Int, languageId: String) {
        if (uri.isEmpty()) return
        languageServer.textDocumentService.didOpen(
            DidOpenTextDocumentParams(
                TextDocumentItem(uri, languageId, version, content)
            )
        )
    }

    override fun changeDocument(uri: String, newContent: String, version: Int) {
        if (uri.isEmpty()) return
        val params = DidChangeTextDocumentParams(
            VersionedTextDocumentIdentifier(uri, version),
            listOf(TextDocumentContentChangeEvent(newContent)),
        )
        languageServer.textDocumentService.didChange(params)
    }

    override fun closeDocument(uri: String) {
        if (uri.isEmpty()) return
        languageServer.textDocumentService.didClose(
            DidCloseTextDocumentParams(TextDocumentIdentifier(uri))
        )
    }

    override fun shutdown(): CompletableFuture<Any> = languageServer.shutdown()

    override fun exit() {
        runCatching { languageServer.exit() }
        connectionManager.close()
    }

    /**
     * Currently, Kotlin-lsp does not take into account client capabilities.
     * Please refer to LSP-223.
     */
    private fun getCompletionCapabilities() = ClientCapabilities().apply {
        textDocument = TextDocumentClientCapabilities().apply {
            completion =
                CompletionCapabilities(
                    CompletionItemCapabilities(true)
                ).apply { contextSupport = true }
        }
        workspace = WorkspaceClientCapabilities().apply { workspaceFolders = true }
    }

    override fun close() {
        super.close()
        connectionManager.close()
    }

    override suspend fun awaitReady(timeout: Duration?) {
        if (timeout == null) {
            readyState.awaitReady()
        } else {
            withTimeout(timeout) { readyState.awaitReady() }
        }
    }

    override fun isReady(): Boolean = readyState.isReady()

    override fun addOnDisconnectListener(listener: () -> Unit) {
        disconnectListeners += listener
    }

    override fun addOnReconnectListener(listener: () -> Unit) {
        reconnectListeners += listener
    }

    companion object {
        const val RECONNECT_TIMEOUT = 60
        const val COMPLETIONS_TIMEOUT = 10
    }
}

private data class ReadyState(
    private val ready: AtomicReference<CompletableDeferred<Unit>> = AtomicReference(CompletableDeferred()),
) {

    @Synchronized
    fun reset(): CompletableDeferred<Unit> {
        if (!ready.get().isCompleted) {
            ready.get().completeExceptionally(IllegalStateException("State reset due to lost connection with LSP"))
        }
        ready.set(CompletableDeferred())
        return ready.get()
    }

    @Synchronized
    fun complete() = ready.get().complete(Unit)

    @Synchronized
    fun fail(t: Throwable) = ready.get().completeExceptionally(t)

    suspend fun awaitReady() = ready.get().await()

    fun isReady() = ready.get().isCompleted
}