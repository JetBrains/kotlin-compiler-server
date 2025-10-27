package completions.configuration.lsp

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Simple LSP configuration properties which expose a way to be accessed
 * even from non-spring managed components, i.e. [completions.lsp.client.LspConnectionManager].
 */
@ConfigurationProperties(prefix = "lsp")
data class LspProperties(
    val host: String,
    val port: Int,
    val reconnectionRetries: Int,
)