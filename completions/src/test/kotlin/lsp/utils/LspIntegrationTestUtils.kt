package lsp.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import completions.lsp.client.KotlinLspClient
import kotlin.time.Duration.Companion.seconds

object LspIntegrationTestUtils {
    const val DEFAULT_LSP_HOST = "localhost"
    const val DEFAULT_LSP_PORT = 9999

    fun waitForLspReady(host: String = DEFAULT_LSP_HOST, port: Int = DEFAULT_LSP_PORT, workspace: String) =
        runBlocking {
            var lastError: Throwable? = null
            withTimeoutOrNull(90.seconds) {
                try {
                    KotlinLspClient(host, port).use { client ->
                        client.initRequest(workspace, projectName = "probe").join()
                    }
                    return@withTimeoutOrNull
                } catch (t: Throwable) {
                    lastError = t
                    delay(1000)
                }
            } ?: error("LSP server did not become ready in time: ${lastError?.message}")
        }
}