package completions.controllers

import completions.service.lsp.LspCompletionProvider
import completions.model.Project
import model.Completion
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/complete"])
class CompletionsRestController(
    private val lspCompletionProvider: LspCompletionProvider
) {
    @PostMapping("/lsp")
    suspend fun complete(
        @RequestBody project: Project,
        @RequestParam line: Int,
        @RequestParam ch: Int,
    ): List<Completion> = lspCompletionProvider.complete(project, line, ch)
}