
package completions.lsp.util.completions

import com.fasterxml.jackson.databind.ObjectMapper
import completions.configuration.JacksonConfig.Companion.walk
import org.eclipse.lsp4j.CompletionItem
import org.springframework.stereotype.Component

@Component
class FuzzyCompletionRanker(
    private val objectMapper: ObjectMapper
) {

    private data class RankedItem(
        val item: CompletionItem,
        val score: Int,
        val matchSpan: Int,
        val candidateLength: Int
    )

    fun List<CompletionItem>.rankedCompletions(): List<CompletionItem> =
        firstOrNull()?.completionQuery(objectMapper)
            ?.takeIf { it.isNotBlank() }
            ?.let { rankCompletions(it) }
            ?: this

    /**
     * Ranking completions inspired by how VS-code does it. Firstly a simple
     * fuzzy scoring is performed on what has been typed by the user so far,
     * then we use [CompletionItem.sortText] to break ties.
     *
     * Tie-breakers after the fuzzy score:
     * - smaller match span (the window covering all matched characters)
     * - shorter candidate length
     * - sortText (ascending)
     *
     * @param query the query the user has typed so far
     */
    fun List<CompletionItem>.rankCompletions(query: String): List<CompletionItem> {
        if (query.isEmpty()) return this.sortedBy { it.sortText }

        return map { item ->
            val candidate = item.sortingKey()
            val (score, span) = fuzzyScoreWithSpan(query, candidate)
            RankedItem(item, score, span, candidate.length)
        }
            .sortedWith(
                compareByDescending<RankedItem> { it.score }
                    .thenBy { it.matchSpan }
                    .thenBy { it.candidateLength }
                    .thenBy { it.item.sortText }
            )
            .map { it.item }
    }

    /**
     * Extracts the prefix of the input [CompletionItem] that triggered the completion.
     */
    private fun CompletionItem.completionQuery(objectMapper: ObjectMapper): String? =
        objectMapper.readTree(data.toString())
            .walk("additionalData")
            ?.walk("prefix")
            ?.asText()

    private fun fuzzyScoreWithSpan(query: String, candidate: String): Pair<Int, Int> {
        if (query.isEmpty()) return 1 to 0

        var score = 0
        var queryIndex = 0
        var lastMatchIndex = -1
        var firstMatchIndex = -1

        for (i in candidate.indices) {
            if (queryIndex >= query.length) break

            val qc = query[queryIndex].lowercaseChar()
            val cc = candidate[i].lowercaseChar()

            if (cc == qc) {
                if (firstMatchIndex == -1) firstMatchIndex = i
                score += 10
                if (lastMatchIndex == i - 1) {
                    score += 5
                } else if (lastMatchIndex != -1) {
                    val gap = i - lastMatchIndex - 1
                    if (gap > 0) score -= gap
                }
                lastMatchIndex = i
                queryIndex++
            }
        }
        return if (queryIndex == query.length) {
            val span = lastMatchIndex - firstMatchIndex + 1
            score to span
        } else {
            0 to Int.MAX_VALUE
        }
    }

    private fun CompletionItem.sortingKey(): String = this.filterText ?: this.label
}