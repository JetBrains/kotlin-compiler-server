package completions.lsp

import org.eclipse.lsp4j.CompletionItem
import model.Completion
import model.Icon
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import model.completionTextFromFullName
import org.eclipse.lsp4j.CompletionItemLabelDetails

object LspCompletionParser {

    /**
     * Converts a `CompletionItem` into a `Completion` by extracting and processing its details.
     */
    fun CompletionItem.toCompletion(): Completion? {
        val (functionParams, importPrefix) = extractParamsAndImportFromLabelDetails(labelDetails)
        if (importPrefix != null && isInternalImport(importPrefix)) return null
        val import = if (hasToBeImported()) importPrefix?.let { "$it.$label"} else null

        return Completion(
            text = completionTextFromFullName(label + functionParams.orEmpty()),
            displayText = label + (labelDetails.detail.orEmpty()),
            tail = labelDetails.description,
            import = import,
            icon = parseIcon(kind?.name)
        )
    }

    /**
     * 4 cases of label details returned by the LSP:
     * - Object, property, ... --> `(qualified.name.here)`
     * - Function/method with params --> `(params: Type, ...) (qualified.name.here)`
     * - *External* object, property, ... --> `for <receiver_type> in qualified.name.here`
     * - *External* function/method --> `<func_name>?(params: Type) for <receiver_type> in qualified.name.here`
     *
     * Now also handles functions with complex parameter lists, including lambdas.
     */
    private fun extractParamsAndImportFromLabelDetails(
        labelDetails: CompletionItemLabelDetails
    ): Pair<String?, String?> {
        val detail = labelDetails.detail ?: return null to null
        return extractFunctionParams(detail) to extractImportFromLabelDetail(detail)
    }

    private fun extractFunctionParams(detail: String): String? {
        // Skip lines that start with whitespace followed by just a package name
        if (detail.matches(Regex("""^\s*\([a-zA-Z.]+\)\s*$""")))  return null

        val regex = Regex("""^\s*((?:\w+\s*)?\([^)]*\)|\([^)]*\)|\w+\s*\{.*?\})""")
        return regex.find(detail)?.groupValues?.get(1)
    }

    private fun extractImportFromLabelDetail(detail: String): String? {
        val regex = Regex("""\(\s*([a-zA-Z0-9_.]+)\s*\)$|for\s+\S+\s+in\s+([a-zA-Z0-9_.]+)""")
        val match = regex.find(detail) ?: return null
        return match.groupValues[1].ifEmpty { match.groupValues[2].ifEmpty { null } }
    }

    /**
     * Icon names differ from [model.Completion] definitions, so we just define
     * a simple mapping between LSP results and current definitions.
     */
    internal fun parseIcon(name: String?): Icon? {
        val iconName = name?.uppercase() ?: return null
        return runCatching { Icon.valueOf(iconName) }
            .getOrElse {
                when (name) {
                    "Interface", "Enum", "Struct" -> Icon.CLASS
                    "Function" -> Icon.METHOD
                    else -> null
                }
            }
    }

    /**
     * Checks whether this completion item has to be imported by parsing additional data
     * of this [CompletionItem]. This additional data may include some useful information mostly
     * related to IntelliJ lookup objects data (e.g. the `importStrategy`) and PSI data.
     */
    private fun CompletionItem.hasToBeImported(): Boolean {
        val lookupObject = objectMapper.parseToJsonElement(data.toString())
            .jsonObject["additionalData"]
            ?.jsonObject?.get("model")
            ?.jsonObject?.get("delegate")
            ?.jsonObject?.get("delegate")
            ?.jsonObject?.get("lookupObject")
            ?.jsonObject?.get("lookupObject")

        val importingStrategy =
            if (lookupObject?.jsonObject?.get("options") != null) {
                lookupObject.jsonObject["options"]
            } else lookupObject

        lookupObject?.jsonObject?.get("kind")?.let {
            if (it.jsonPrimitive.content.contains("PackagePart")) return false
        }

        return importingStrategy
            ?.jsonObject?.get("importingStrategy")
            ?.jsonObject?.get("kind")
            ?.jsonPrimitive?.content?.contains("DoNothing")?.not()
            ?: true
    }

    private fun isInternalImport(import: String): Boolean =
        import.substringBeforeLast('.').let {
            it in excludeFromCompletion || excludeFromCompletion.any { prefix -> it.startsWith(prefix) }
        }

    // TODO(Stefano Furi): include what was excluded in previous completion results
    private val excludeFromCompletion = listOf(
        "jdk.internal",
    )
}

internal object FuzzyCompletionRanking {
    private data class RankedItem(val item: CompletionItem, val score: Int)

    /**
     * Extracts the prefix of this [CompletionItem] that triggered the completion.
     */
    val CompletionItem.completionQuery: String?
        get() = objectMapper.parseToJsonElement(data.toString()).jsonObject["additionalData"]
                ?.jsonObject?.get("prefix")
                ?.jsonPrimitive?.content

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
}

private val objectMapper = Json { ignoreUnknownKeys = true }
