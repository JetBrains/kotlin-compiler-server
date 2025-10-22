package completions.configuration.lsp

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Simple LSP configuration properties which expose a way to be accessed
 * even from non-spring managed components, i.e. [completions.lsp.client.LspConnectionManager].
 */
@Component
@ConfigurationProperties(prefix = "lsp")
data class LspProperties(
    var host: String = DEFAULT_LSP_HOST,
    var port: Int = DEFAULT_LSP_PORT,
    var reconnectionRetries: Int = DEFAULT_RECONNECTION_RETRIES,
) {
    companion object {
        private const val DEFAULT_LSP_HOST = "127.0.0.1"
        private const val DEFAULT_LSP_PORT = 9999
        private const val DEFAULT_RECONNECTION_RETRIES = 10

        private var instance: LspProperties? = null

        internal fun setInstance(props: LspProperties) {
            instance = props
        }

        fun getHost(): String = instance?.host
            ?: System.getProperty("LSP_HOST")
            ?: System.getenv("LSP_HOST")
            ?: DEFAULT_LSP_HOST

        fun getPort(): Int = instance?.port
            ?: System.getProperty("LSP_PORT")?.toIntOrNull()
            ?: System.getenv("LSP_PORT")?.toIntOrNull()
            ?: DEFAULT_LSP_PORT

        fun getReconnectionRetries(): Int =instance?.reconnectionRetries
            ?: DEFAULT_RECONNECTION_RETRIES
    }

    init {
        setInstance(this)
    }
}