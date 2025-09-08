package completions

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CompletionsApplication

fun main(args: Array<String>) {
    runApplication<CompletionsApplication>(*args)
}
