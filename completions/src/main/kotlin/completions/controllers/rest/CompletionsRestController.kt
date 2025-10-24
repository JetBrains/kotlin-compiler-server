package completions.controllers.rest

import completions.dto.api.CompletionRequest
import completions.dto.api.CompletionResponse
import completions.service.lsp.LspCompletionQueue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/compiler"])
class CompletionsRestController(
    private val lspCompletionQueue: LspCompletionQueue,
) {
    @PostMapping("/complete")
    suspend fun complete(
        @RequestBody completionRequest: CompletionRequest,
        @RequestParam line: Int,
        @RequestParam ch: Int,
    ): List<CompletionResponse> {
        return lspCompletionQueue.complete(completionRequest, line, ch)
    }
}