package completions.controllers.rest

import completions.dto.api.CompletionRequest
import completions.service.lsp.LspCompletionProvider
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
        @RequestBody completionRequest: CompletionRequest,
        @RequestParam line: Int,
        @RequestParam ch: Int,
    ): List<Completion> = lspCompletionProvider.complete(completionRequest, line, ch)
}