package completions.lsp.client

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.ConnectException
import java.net.Socket
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds


internal class LspConnectionManager(
    private val host: String = lspHost(),
    private val port: Int = lspPort(),
    private val languageClient: LanguageClient = KotlinLanguageClient(),
    private val maxConnectionRetries: Int = 10,
): AutoCloseable {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Volatile
    private var socket: Socket? = null
    @Volatile
    private var serverProxy: LanguageServer? = null
    @Volatile
    private var listenFuture: Future<Void>? = null

    private val isClosing = AtomicBoolean(false)
    private val reconnecting = AtomicBoolean(false)

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val disconnectListeners = mutableListOf<() -> Unit>()
    private val reconnectListeners = mutableListOf<() -> Unit>()

    /**
     * Connects to the language server, if not already connected.
     *
     * @param initial Whether this is the initial connection attempt. If false,
     *                all reconnection listeners will be notified.
     */
    @Synchronized
    fun ensureConnected(initial: Boolean = false): LanguageServer {
        if (isClosing.get()) error("Connection manager is closing or already closed")
        serverProxy?.let { return it }
        runBlocking { connectWithRetry(initial) }
        return serverProxy ?: error("Could not connect to LSP server")
    }

    private suspend fun connectWithRetry(initial: Boolean = false) {
        var attempt = 0
        var lastError: Exception? = null
        while (attempt < maxConnectionRetries && !isClosing.get()) {
            try {
                connectOnce(initial)
                return
            } catch (e: Exception) {
                lastError = e
                if (e is ConnectException || e.cause is ConnectException) {
                    logger.info("Could not connect to LSP server: ${e.message}")
                } else {
                    logger.warn("Unexpected error while connecting to LSP server:", e)
                }
                logger.info("Trying reconnect, attempt {} of {}", attempt, maxConnectionRetries)
                delay(exponentialBackoffMillis(attempt++).milliseconds)
            }
        }
        throw ConnectException("Could not connect to LSP server after $maxConnectionRetries attempts").apply {
            if (lastError != null) initCause(lastError)
        }
    }

    private suspend fun connectOnce(initial: Boolean = false) = withContext(Dispatchers.IO) {
        val s = Socket(host, port).apply {
            tcpNoDelay = true
            keepAlive = true
            soTimeout = 0
            setPerformancePreferences(0, 2, 1)
        }
        val input = BufferedInputStream(s.getInputStream())
        val output = BufferedOutputStream(s.getOutputStream())
        val launcher = LSPLauncher.createClientLauncher(languageClient, input, output)
        launcher.startListening().also {
            listenFuture = it
            watchConnection(it)
        }
        socket = s
        serverProxy = launcher.remoteProxy
        if (!initial) notifyReconnected()
        logger.info("Connected to LSP server at {}:{}, awaiting initialization result", host, port)
    }

    private fun watchConnection(future: Future<Void>) {
        scope.launch {
            try {
                future.get()
            } catch (t: Throwable) {
                if (t !is CancellationException) {
                    logger.info("LSP connection listening job finished unexpectedly:", t)
                }
            } finally {
                handleDisconnectAndReconnect()
            }
        }
    }

    private fun handleDisconnectAndReconnect() {
        if (isClosing.get()) return
        if (!reconnecting.compareAndSet(false, true)) {
            logger.debug("Reconnect already in progress, skipping duplicate trigger")
            return
        }

        notifyDisconnected()
        tearDown()

        scope.launch {
            var attempt = 0
            try {
                while (!isClosing.get() && attempt < maxConnectionRetries) {
                    try {
                        if (attempt > 0) delay(exponentialBackoffMillis(attempt).milliseconds)
                        connectWithRetry()
                        return@launch
                    } catch (t: Throwable) {
                        logger.info("Reconnect attempt {} failed: {}", ++attempt, t.message)
                    }
                }
            } finally {
                reconnecting.set(false)
            }
        }
    }

    private fun notifyDisconnected() = notify(disconnectListeners)

    private fun notifyReconnected() = notify(reconnectListeners)

    private fun notify(listeners: List<() -> Unit>) {
        listeners.forEach {
            runCatching { it() }
        }
    }

    private fun tearDown() {
        runCatching { listenFuture?.cancel(true) }
        runCatching { socket?.close() }
        listenFuture = null
        socket = null
        serverProxy = null
    }

    fun addOnDisconnectListener(listener: () -> Unit) {
        disconnectListeners += listener
    }

    fun addOnReconnectListener(listener: () -> Unit) {
        reconnectListeners += listener
    }

    override fun close() {
        isClosing.set(true)
        tearDown()
        scope.cancel()
    }

    companion object {
        private const val LSP_DEFAULT_HOST = "127.0.0.1"
        private const val LSP_DEFAULT_PORT = 9999

        fun lspHost(): String =
            System.getProperty("LSP_HOST")
                ?: System.getenv("LSP_HOST")
                ?: LSP_DEFAULT_HOST

        fun lspPort(): Int =
            System.getProperty("LSP_PORT")?.toInt()
                ?: System.getenv("LSP_PORT")?.toInt()
                ?: LSP_DEFAULT_PORT

        /**
         * Basic exponential backoff starting from [base]ms with jitter (+/-[jitterFactor]%), up to [maxVal]ms
         *
         * @param attempt The attempt number (starting at 0)
         * @param base The base value in millis
         * @param maxVal The maximum value in millis
         * @param jitterFactor The jitter factor (0.0 to 1.0)
         * @return The exponential backoff value in milliseconds
         */
        internal fun exponentialBackoffMillis(
            attempt: Int,
            base: Double = 1000.0,
            maxVal: Double = 60 * base,
            jitterFactor: Double = 0.3
        ): Double {
            val backoff = base * 3.0.pow(attempt)
            val jitter = Random.nextDouble(from = -jitterFactor, until = jitterFactor)
            val withJitter = backoff * (1.0 + jitter)
            return withJitter.coerceAtMost(maxVal)
        }
    }
}