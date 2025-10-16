package completions.lsp.util.completions

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.eclipse.lsp4j.CompletionItem

internal object FuzzyCompletionRanking {
    private val objectMapper = Json { ignoreUnknownKeys = true }

    private data class RankedItem(val item: CompletionItem, val score: Int)

    /**
     * Extracts the prefix of this [CompletionItem] that triggered the completion.
     */
    val CompletionItem.completionQuery: String?
        get() = objectMapper.parseToJsonElement(data.toString()).jsonObject["additionalData"]
            ?.jsonObject?.get("prefix")
            ?.jsonPrimitive?.content

    /**
     * Ranking completions inspired by how VS-code does it. Firstly a simple
     * fuzzy scoring is performed on what has been typed by the user so far,
     * then we use [CompletionItem.sortText] to break ties.
     *
     * @param query the query the user has typed so far
     */
    fun List<CompletionItem>.rankCompletions(query: String): List<CompletionItem> =
        map { RankedItem(it, fuzzyScore(query, it.sortingKey())) }
            .sortedWith(compareByDescending<RankedItem> { it.score }.thenBy { it.item.sortText })
            .map { it.item }

    private fun fuzzyScore(query: String, candidate: String): Int {
        if (query.isEmpty()) return 1

        var score = 0
        var queryIndex = 0
        var lastMatchIndex = -1

        for (i in candidate.indices) {
            if (queryIndex >= query.length) break

            val qc = query[queryIndex].lowercaseChar()
            val cc = candidate[i].lowercaseChar()

            if (cc == qc) {
                score += 10
                if (lastMatchIndex == i - 1) score += 5 // consecutive match bonus
                if (i == 0 || !candidate[i-1].isLetterOrDigit()) score += 3 // bonus if beginning
                lastMatchIndex = i
                queryIndex++
            }
        }
        return if (queryIndex == query.length) score else 0
    }

    private fun CompletionItem.sortingKey(): String = this.filterText ?: this.label
}

