package completions.lsp.client

import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.services.LanguageClient
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * Basic implementation of [LanguageClient], which just logs all received messages.
 */
class KotlinLanguageClient : LanguageClient {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun telemetryEvent(o: Any) {
        logger.debug(o.toString())
    }

    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams) {
        diagnostics.diagnostics.forEach { d -> logger.debug("Diagnostic: {} at {}", d.message, d.range) }
    }

    override fun showMessage(messageParams: MessageParams) {
        logger.debug("[{}]: {}", messageParams.type, messageParams.message)
    }

    override fun showMessageRequest(params: ShowMessageRequestParams): CompletableFuture<MessageActionItem?>? {
        logger.debug("Message request: ${params.message}")
        params.actions?.forEach { action ->
            logger.debug("Action: ${action.title}")
        }
        return CompletableFuture.completedFuture(params.actions?.firstOrNull())
    }

    override fun logMessage(message: MessageParams) {
        logger.debug(message.message)
    }
}
