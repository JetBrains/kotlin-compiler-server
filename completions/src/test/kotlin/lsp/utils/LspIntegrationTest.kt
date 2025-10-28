package lsp.utils

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@ExtendWith(KotlinLspComposeExtension::class)
open class LspIntegrationTest {
    companion object {

        @Suppress("UNUSED")
        @JvmStatic
        @DynamicPropertySource
        fun registerLspProperties(registry: DynamicPropertyRegistry) {
            val host = System.getProperty("LSP_HOST") ?: "localhost"
            val port = System.getProperty("LSP_PORT")?.toInt() ?: 9999
            registry.add("lsp.host") { host }
            registry.add("lsp.port") { port }
            registry.add("lsp.reconnection-retries") { 10 }
        }
    }
}