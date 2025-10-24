package lsp.utils

import completions.lsp.client.KotlinLspClient
import completions.lsp.client.LspConnectionManager
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.milliseconds

object LspIntegrationTestUtils {
    const val DEFAULT_LSP_HOST = "localhost"
    const val DEFAULT_LSP_PORT = 9999

    fun waitForLspReady(host: String = DEFAULT_LSP_HOST, port: Int = DEFAULT_LSP_PORT, workspace: String) {
        var lastError: Throwable?
        var attempt = 0
        while (true) {
            try {
                KotlinLspClient(host, port).use { client ->
                    client.initRequest(workspace, projectName = "probe").get(90, TimeUnit.SECONDS)
                }
                return
            } catch (t: Throwable) {
                lastError = t
                if (t is TimeoutException) error("LSP server never started: $lastError")
                Thread.sleep(LspConnectionManager.exponentialBackoffMillis(attempt++).milliseconds.inWholeMilliseconds)
            }
        }
    }
}