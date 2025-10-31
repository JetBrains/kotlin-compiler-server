package completions

import completions.configuration.lsp.LspProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(value = [LspProperties::class])
class CompletionsApplication

fun main(args: Array<String>) {
    runApplication<CompletionsApplication>(*args)
}
