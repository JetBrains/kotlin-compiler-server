package completions.lsp

import completions.configuration.lsp.LspProperties
import completions.dto.api.CompletionRequest
import completions.lsp.StatefulKotlinLspProxy.getCompletionsForClient
import completions.lsp.StatefulKotlinLspProxy.onClientConnected
import completions.lsp.StatefulKotlinLspProxy.onClientDisconnected
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.CompletionItem
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * A component for managing multiple versions of the [Kotlin LSP proxies][KotlinLspProxy].
 * This class facilitates multi-version Kotlin code autocompletion requests by dynamically
 * handling and initializing Kotlin LSP proxies for the required Kotlin versions.
 */
@Component
class MultipleVersionsKotlinLspProxy(
    private val properties: LspProperties,
) {
    private val proxyCoroutineScope =
        CoroutineScope(Dispatchers.IO + CoroutineName("MultipleVersionsKotlinLspProxy"))

    private val proxies: MutableMap<String, Deferred<KotlinLspProxy>> = ConcurrentHashMap()

    @EventListener(ApplicationReadyEvent::class)
    fun initDefaultClient() {
        proxyCoroutineScope.launch {
            this@MultipleVersionsKotlinLspProxy[properties.kotlinVersion]
        }
    }

    /**
     * @see [KotlinLspProxy.getOneTimeCompletions]
     */
    suspend fun getOneTimeCompletions(
        request: CompletionRequest,
        kotlinVersion: String,
        line: Int,
        ch: Int
    ): List<CompletionItem> = this[kotlinVersion].getOneTimeCompletions(request, line, ch)

    /**
     * @see [StatefulKotlinLspProxy.getCompletionsForClient]
     */
    suspend fun getCompletionsForClient(
        clientId: String,
        request: CompletionRequest,
        kotlinVersion: String,
        line: Int,
        ch: Int,
    ): List<CompletionItem> = this[kotlinVersion].getCompletionsForClient(clientId, request, line, ch)

    /**
     * @see [StatefulKotlinLspProxy.onClientConnected]
     */
    suspend fun onClientConnected(clientId: String, kotlinVersion: String = properties.kotlinVersion) =
        this[kotlinVersion].onClientConnected(clientId)

    /**
     * @see [StatefulKotlinLspProxy.onClientDisconnected]
     */
    suspend fun onClientDisconnected(clientId: String, kotlinVersion: String = properties.kotlinVersion) =
        this[kotlinVersion].onClientDisconnected(clientId)

    /**
     * @see [KotlinLspProxy.requireAvailable]
     */
    suspend fun requireAvailable(kotlinVersion: String = properties.kotlinVersion) = this[kotlinVersion]

    private suspend fun retrieveProxy(kotlinVersion: String): KotlinLspProxy =
        proxies.computeIfAbsent(kotlinVersion) {
            proxyCoroutineScope.async {
                KotlinLspProxy(properties.forKotlinVersion(kotlinVersion)).also {
                    it.initializeClient(clientName = "kotlin-compiler-server-$kotlinVersion")
                }
            }
        }.await()

    private suspend operator fun get(key: String): KotlinLspProxy = retrieveProxy(key).also { it.requireAvailable() }
}