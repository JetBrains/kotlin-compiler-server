package completions.service.lsp

import completions.dto.api.Completion
import completions.dto.api.CompletionRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

/**
 * Manages a queue for handling LSP code completion requests using background workers.
 *
 * Each worker is initialized to await the readiness of the [LspCompletionProvider] before processing requests.
 * Code completion requests are submitted via the [complete] function and are processed asynchronously,
 * while returned to the caller through a [CompletableDeferred].
 */
@Component
class LspCompletionQueue(
    private val provider: LspCompletionProvider,
    workerContext: CoroutineContext,
) {
    private val queue = Channel<CompletionJob>(capacity = 256)
    private val scope = CoroutineScope(SupervisorJob() + workerContext)
    private val nWorkers = (Runtime.getRuntime().availableProcessors()).coerceAtLeast(2)

    init {
        repeat(nWorkers) {
            scope.launch {
                provider.awaitReady()
                for (job in queue) {
                    runCatching {
                        provider.awaitReady()
                        provider.complete(job.request, job.line, job.ch)
                    }.fold(
                        onSuccess = { job.result.complete(it) },
                        onFailure = { job.result.completeExceptionally(it) }
                    )
                }
            }
        }
    }

    /**
     * Handles a code completion request by adding it to a processing queue and awaiting the result.
     *
     * The request is submitted as a [CompletionJob], which is asynchronously processed by a worker.
     * Once processed, the result is returned as a list of [Completion]s.
     *
     * @param request the [CompletionRequest] object containing the necessary context for code completion
     * @param line the line number in the file for which completions are to be provided
     * @param ch the character position within the specified line for determining completions
     * @return a list of [Completion]s corresponding to the provided position in the file
     */
    suspend fun complete(request: CompletionRequest, line: Int, ch: Int): List<Completion> {
        val deferred = CompletableDeferred<List<Completion>>()
        queue.send(CompletionJob(request, line, ch, deferred))
        return deferred.await()
    }

    private data class CompletionJob(
        val request: CompletionRequest,
        val line: Int,
        val ch: Int,
        val result: CompletableDeferred<List<Completion>>,
    )
}