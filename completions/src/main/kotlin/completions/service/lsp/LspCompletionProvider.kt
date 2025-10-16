package completions.service.lsp

import completions.dto.api.CompletionRequest
import completions.dto.api.ProjectFile
import completions.lsp.FuzzyCompletionRanking.completionQuery
import completions.lsp.FuzzyCompletionRanking.rankCompletions
import completions.lsp.KotlinLspProxy
import completions.lsp.LspCompletionParser.toCompletion
import completions.lsp.StatefulKotlinLspProxy.getCompletionsForClient
import completions.dto.api.Completion
import org.eclipse.lsp4j.CompletionItem
import org.springframework.stereotype.Component

/**
 * Provides code completion functionality by leveraging an LSP server.
 *
 * TODO(KTL-3757): refactor this class to select user's selected file for completions inside the given project.
 */
@Component
class LspCompletionProvider(
    private val lspProxy: KotlinLspProxy,
) {

    /**
     * Retrieves a list of code completions for a specified position within a project file.
     * The completions are fetched and processed, with an optional fuzzy ranking applied.
     *
     * @param request the request containing the files
     * @param line the line number within the file where completions are to be provided
     * @param ch the character position within the line for the completions
     * @param applyFuzzyRanking whether to apply fuzzy ranking to the completions
     * @return a list of [Completion]s relevant to the specified position in the file
     */
    suspend fun complete(
        request: CompletionRequest,
        line: Int,
        ch: Int,
        applyFuzzyRanking: Boolean = true
    ): List<Completion> =
        lspProxy.getOneTimeCompletions(request, line, ch).transformCompletions(request, applyFuzzyRanking)

    /**
     * Overload of [complete] that accepts a client ID for stateful scenarios.
     */
    suspend fun complete(
        clientId: String,
        request: CompletionRequest,
        line: Int,
        ch: Int,
        applyFuzzyRanking: Boolean = true
    ): List<Completion> =
        lspProxy.getCompletionsForClient(clientId, request, line, ch).transformCompletions(request, applyFuzzyRanking)

    private fun List<CompletionItem>.transformCompletions(
        request: CompletionRequest,
        applyFuzzyRanking: Boolean
    ): List<Completion> =
        if (applyFuzzyRanking) {
            rankedCompletions()
        } else {
            this
        }.mapNotNull { it.toCompletion() }.cleanupImports(request.files.first())

    private fun List<CompletionItem>.rankedCompletions(): List<CompletionItem> =
        firstOrNull()?.completionQuery
            ?.takeIf { !it.isBlank() }
            ?.let { rankCompletions(it) }
            ?: this


    /**
     * Transform a list of [Completion]s to a list of [CompletionItem]s, with the following changes:
     * - Remove any imports if already imported in the project
     * - Add the `hasOtherImports` flag to any completions that have the same name as an already-imported class
     */
    private fun List<Completion>.cleanupImports(file: ProjectFile): List<Completion> {
        val imports = extractImports(file)
        return map { completion ->
            if (completion.import != null && completion.import in imports) {
                completion.copy(import = null)
            } else if (imports.any { it.endsWith(completion.text) }) {
                completion.copy(text = getTextWhenHasOtherImports(completion), import = null, hasOtherImports = true)
            } else {
                completion
            }
        }
    }

    /**
     * Right now the only reasonable way to extract imports is to parse the imports through a regex. If analysis
     * tools will be used (e.g. PSI), this will have to be revisited.
     *
     * Please note that this method does not check for some edge cases, more notably:
     * - Star imports
     * - Aliased imports
     */
    private fun extractImports(file: ProjectFile): Set<String> {
        val importsPattern = """^\s*import\s+([\w.*]+)""".toRegex(RegexOption.MULTILINE)
        return importsPattern.findAll(file.text)
            .map { it.groupValues[1].trim() }
            .toSet()
    }

    private fun getTextWhenHasOtherImports(completion: Completion) =
        completion.import?.substringBeforeLast('.') + '.' + completion.text
}