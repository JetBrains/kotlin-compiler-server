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