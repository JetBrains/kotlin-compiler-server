package completions.lsp

import org.eclipse.lsp4j.CompletionItem
import completions.dto.api.Completion
import completions.dto.api.Icon
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.eclipse.lsp4j.CompletionItemLabelDetails

object LspCompletionParser {

    /**
     * Converts a `CompletionItem` into a `Completion` by extracting and processing its details.
     */
    fun CompletionItem.toCompletion(): Completion? {
        val (functionParams, importPrefix) = extractParamsAndImportFromLabelDetails(labelDetails)
        if (importPrefix != null && isInternalImport(importPrefix)) return null
        val hasToBeImported = hasToBeImported()
        val import = if (hasToBeImported) importPrefix?.let { "$it.$label"} else null
        val detail = labelDetails.detail?.let { removeImportFromDetail(it, hasToBeImported) }

        return Completion(
            text = Completion.completionTextFromFullName(label + functionParams.orEmpty()),
            displayText = label + detail,
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

    private fun removeImportFromDetail(detail: String, hasToBeImported: Boolean): String {
        if (hasToBeImported) return detail

        // ` (qualified.name.here)`
        val onlyImport = Regex("""^\s*\(\s*[a-zA-Z0-9_.]+\s*\)\s*$""")
        if (onlyImport.matches(detail)) return ""

        var result = detail

        // ``(params) (qualified.name.here)`
        result = result.replace(Regex("""\s*\(\s*[a-zA-Z0-9_.]+\s*\)\s*$"""), "")

        // `... for <receiver> in qualified.name.here`
        result = result.replace(Regex("""\s+for\s+\S+\s+in\s+[a-zA-Z0-9_.]+\s*$"""), "")

        return result
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

    private val excludeFromCompletion = listOf(
        "jdk.internal",
        "kotlin.jvm.internal",
        "kotlin.coroutines.experimental.intrinsics",
        "kotlin.coroutines.intrinsics",
        "kotlin.coroutines.experimental.jvm.internal",
        "kotlin.coroutines.jvm.internal",
        "kotlin.reflect.jvm.internal",
    )
}

private val objectMapper = Json { ignoreUnknownKeys = true }
