package completions.lsp

import completions.exceptions.LspUnavailableException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class LspAvailabilityAdvice {
    @ExceptionHandler(LspUnavailableException::class)
    fun handleLspUnavailable(ex: LspUnavailableException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf("error" to ex.message))
}
