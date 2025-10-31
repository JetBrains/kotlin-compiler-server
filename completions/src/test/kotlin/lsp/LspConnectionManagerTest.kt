package lsp

import completions.lsp.client.LspConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import lsp.utils.TestLspServer
import lsp.utils.TestLspServer.Companion.useSuspend
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LspConnectionManagerTest {

    @Test
    fun `ensureConnected connects to running server and notifies initial reconnect`() = runBlocking {
        var reconnects = 0
        TestLspServer.withTestLspServer {
            LspConnectionManager(HOST, boundPort).use { manager ->
                manager.addOnReconnectListener { ++reconnects }
                val ls = manager.ensureConnected()
                assertNotNull(ls) {
                    "LanguageServer should not be null after ensureConnected()"
                }
                eventually(2.seconds) { reconnects >= 1 }
            }
        }
    }

    @Test
    fun `manager auto-reconnects after abrupt server-side-disconnect`() = runBlocking {
        var disconnects = 0
        var reconnects = 0
        TestLspServer.withTestLspServer {
            LspConnectionManager(HOST, boundPort).use { manager ->
                manager.addOnDisconnectListener { ++disconnects }
                manager.addOnReconnectListener { ++reconnects }
                manager.ensureConnected()
                eventually(2.seconds) { reconnects >= 1}
                closeActiveConnection()
                eventually(5.seconds) { disconnects >= 1 && reconnects >= 2}
            }
        }
    }

    @Test
    fun `ensureConnected succeeds when server starts after a while`() = runBlocking {
        val delayedServer = TestLspServer(0) // server is not listening (`startAccepting()`)
        val port = delayedServer.boundPort

        LspConnectionManager(HOST, port, maxConnectionRetries = 50).use { manager ->
            try {
                val connectDeferred = async(Dispatchers.IO) { manager.ensureConnected() }
                delay(1.seconds)
                delayedServer.startAccepting()

                val ls = connectDeferred.await()
                assertNotNull(ls) {
                    "LanguageServer should not be null after delayed server start"
                }
            } finally {
                delayedServer.close()
            }
        }
    }

    @Test
    fun `ensureConnected is single-flight and debounces concurrent attempts`() = runBlocking {
        TestLspServer.withTestLspServer {
            LspConnectionManager(HOST, boundPort).useSuspend { manager ->
                val jobs = (1..16).map {
                    async(Dispatchers.IO) { manager.ensureConnected() }
                }

                val results = jobs.awaitAll()
                val first = results.first()

                results.forEach { assertSame(first, it, "All ensureConnected() results must be the same instance") }

                val second = manager.ensureConnected()
                assertSame(first, second, "Subsequent ensureConnected() must return the same instance")
            }
        }
    }

    companion object {
        private const val HOST = "localhost"

        private fun eventually(timeout: Duration, poll: Duration = 50.milliseconds, condition: () -> Boolean) {
            val start = System.nanoTime()
            while (!condition()) {
                if ((System.nanoTime() - start) / 1_000_000 >= timeout.inWholeMilliseconds) {
                    assertTrue(condition()) {
                        "Condition not satisfied within $timeout"
                    }
                    return
                }
                Thread.sleep(poll.inWholeMilliseconds)
            }
        }
    }
}