package completions.exceptions.advice

import completions.exceptions.LspUnavailableException
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        ex: ResponseStatusException,
        request: ServerHttpRequest,
    ): Mono<ResponseEntity<ProblemDetail>> {
        val status = HttpStatus.resolve(ex.statusCode.value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
        val message = "ResponseStatusException: ${ex.message}:\n${ex.stackTraceToString()}"
        if (status.is5xxServerError) logger.error(message) else logger.warn(message)

        val problem = problem(
            status = status,
            request = request,
            detail = ex.reason ?: ex.message,
            title = status.reasonPhrase
        )
        return Mono.just(ResponseEntity.status(status).body(problem))
    }

    @ExceptionHandler(LspUnavailableException::class)
    fun handleLspUnavailableException(
        ex: LspUnavailableException,
        request: ServerHttpRequest
    ): Mono<ResponseEntity<ProblemDetail>> {
        logger.warn("LSP unavailable: ${ex.message}")
        val status = HttpStatus.SERVICE_UNAVAILABLE
        val problem = problem(
            status = status,
            request = request,
            detail = ex.message,
            title = "Service Unavailable"
        )
        return Mono.just(ResponseEntity.status(status).body(problem))
    }

    @ExceptionHandler(Throwable::class)
    fun handleAny(
        ex: Throwable,
        request: ServerHttpRequest,
    ): Mono<ResponseEntity<ProblemDetail>> {
        logger.error("Unhandled exception: ${ex.message}", ex)
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        val problem = problem(
            status = status,
            request = request,
            detail = "${ex.message}:\n${ex.stackTraceToString()}",
            title = "Internal Server Error"
        )
        return Mono.just(ResponseEntity.status(status).body(problem))
    }

    private fun problem(
        status: HttpStatus,
        request: ServerHttpRequest,
        detail: String?,
        title: String = status.reasonPhrase,
    ): ProblemDetail {
        val problemDetail = ProblemDetail.forStatusAndDetail(status, detail ?: status.reasonPhrase)
        problemDetail.title = title
        problemDetail.setProperty("path", request.uri.path)
        return problemDetail
    }
}