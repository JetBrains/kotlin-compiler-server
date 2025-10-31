package lsp.utils

import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class TestLspServer(port: Int = 0): AutoCloseable {
    private val acceptLoopRunning = AtomicBoolean(false)
    private val clientSocketRef = AtomicReference<Socket?>()
    private val listeningFut = AtomicReference<Future<Void>?>()

    private val srvSock = ServerSocket(port)
    val boundPort: Int get() = srvSock.localPort

    private val serverImpl = object : LanguageServer, LanguageClientAware {
        private var client: LanguageClient? = null

        override fun initialize(params: InitializeParams?): CompletableFuture<InitializeResult> =
            CompletableFuture.completedFuture(InitializeResult(ServerCapabilities()))

        override fun shutdown(): CompletableFuture<in Any> = CompletableFuture.completedFuture(null)

        override fun exit() { }

        override fun getTextDocumentService(): TextDocumentService = object : TextDocumentService {
            override fun didOpen(params: DidOpenTextDocumentParams?) {}
            override fun didChange(params: DidChangeTextDocumentParams?) {}
            override fun didClose(params: DidCloseTextDocumentParams?) {}
            override fun didSave(params: DidSaveTextDocumentParams?) {}
        }

        override fun getWorkspaceService(): WorkspaceService = object : WorkspaceService {
            override fun didChangeConfiguration(params: DidChangeConfigurationParams?) { }
            override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams?) { }
        }

        override fun connect(client: LanguageClient) { this.client = client }
    }

    fun startAccepting() {
        if (!acceptLoopRunning.compareAndSet(false, true)) return
        thread(name = "test-lsp-accept-loop", isDaemon = true) {
            while (acceptLoopRunning.get()) {
                try {
                    val socket = srvSock.accept()
                    clientSocketRef.getAndSet(socket)?.let { safeClose(it) }
                    val input = BufferedInputStream(socket.getInputStream())
                    val output = BufferedOutputStream(socket.getOutputStream())
                    val launcher = LSPLauncher.createServerLauncher(serverImpl, input, output)
                    launcher.startListening().also {
                        listeningFut.getAndSet(it)?.cancel(true)
                    }
                } catch (_: Throwable) {
                    if (!acceptLoopRunning.get()) break
                }
            }
        }
    }

    fun closeActiveConnection() {
        clientSocketRef.getAndSet(null)?.let { safeClose(it) }
        listeningFut.getAndSet(null)?.cancel(true)
    }

    override fun close() {
        acceptLoopRunning.set(false)
        closeActiveConnection()
        safeClose(srvSock)
    }

    private fun safeClose(closeable: AutoCloseable) {
        runCatching { closeable.close() }
    }

    companion object {
        suspend fun withTestLspServer(port: Int = 0, body: suspend TestLspServer.() -> Unit) {
            TestLspServer(port).useSuspend { server ->
                server.startAccepting()
                body(server)
            }
        }

        suspend inline fun <T : AutoCloseable?, R> T.useSuspend(block: suspend (T) -> R): R {
            var closed = false
            try {
                return block(this)
            } catch (e: Exception) {
                closed = true
                try {
                    this?.close()
                } catch (closeException: Exception) {
                    e.addSuppressed(closeException)
                }
                throw e
            } finally {
                if (!closed) {
                    this?.close()
                }
            }
        }

    }
}