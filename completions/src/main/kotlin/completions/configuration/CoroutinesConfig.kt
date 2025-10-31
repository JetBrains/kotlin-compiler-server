package completions.configuration

import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

@Configuration
class CoroutinesConfig {
    @Bean
    fun lspCoroutineContext(): CoroutineContext {
        val parallelism = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(4)
        return Executors.newFixedThreadPool(parallelism).asCoroutineDispatcher()
    }
}