package completions.configuration

import completions.controllers.ws.LspCompletionWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
class WebSocketConfiguration {
    @Bean
    fun webSocketHandlerAdapter(): WebSocketHandlerAdapter = WebSocketHandlerAdapter()

    @Bean
    fun webSocketMapping(handler: LspCompletionWebSocketHandler): HandlerMapping =
        SimpleUrlHandlerMapping(mapOf(WEBSOCKET_COMPLETIONS_PATH to handler), 1)

    companion object {
        const val WEBSOCKET_COMPLETIONS_PATH = "/api/complete"
    }
}