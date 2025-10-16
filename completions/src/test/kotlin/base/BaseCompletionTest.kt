package base

import completions.model.Project
import completions.model.ProjectFile
import model.Completion
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

interface BaseCompletionTest {
    fun performCompletionChecks(
        code: String,
        line: Int,
        character: Int,
        expected: List<String>,
        isJs: Boolean = false
    )

    companion object {
        fun WebTestClient.retrieveCompletions(url: String, code: String): List<Completion> {
            val project = Project(files = listOf(ProjectFile(text = code, name = "file.kt")))
            return withTimeout {
                post()
                    .uri(url)
                    .bodyValue(project)
                    .exchange()
                    .expectStatus().isOk
                    .expectBodyList(Completion::class.java)
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