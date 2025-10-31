package base

import completions.dto.api.ProjectFile
import completions.lsp.components.LspProject
import completions.dto.api.CompletionResponse
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

interface BaseCompletionTest {
    fun performCompletionChecks(
        codeWithCaret: String,
        expected: List<String>,
        isJs: Boolean = false
    )

    companion object Utils {
        fun WebTestClient.retrieveCompletions(url: String, code: String): List<CompletionResponse> {
            val project = LspProject(files = listOf(ProjectFile(text = code, name = "file.kt")))
            return withTimeout {
                post()
                    .uri(url)
                    .bodyValue(project)
                    .exchange()
                    .expectStatus().isOk
                    .expectBodyList(CompletionResponse::class.java)
                    .returnResult()
                    .responseBody
            } ?: emptyList()
        }

        private fun <T> WebTestClient.withTimeout(
            duration: Duration = 2.minutes.toJavaDuration(),
            body: WebTestClient.() -> T?
        ): T? = with(mutate().responseTimeout(duration).build()) { body() }
    }
}