package lsp.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import completions.lsp.client.KotlinLspClient
import completions.lsp.client.LspConnectionManager
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object LspIntegrationTestUtils {
    const val DEFAULT_LSP_HOST = "localhost"
    const val DEFAULT_LSP_PORT = 9999

    fun waitForLspReady(host: String = DEFAULT_LSP_HOST, port: Int = DEFAULT_LSP_PORT, workspace: String) =
        runBlocking {
            var lastError: Throwable? = null
            var attempt = 0
            withTimeoutOrNull(90.seconds) {
                while (true) {
                    try {
                        KotlinLspClient(host, port).use { client ->
                            client.initRequest(workspace, projectName = "probe").join()
                        }
                        return@withTimeoutOrNull
                    } catch (t: Throwable) {
                        lastError = t
                        delay(LspConnectionManager.exponentialBackoffMillis(attempt++).milliseconds)
                    }
                }
            } ?: error("LSP server did not become ready in time: ${lastError?.message}")
        }
}