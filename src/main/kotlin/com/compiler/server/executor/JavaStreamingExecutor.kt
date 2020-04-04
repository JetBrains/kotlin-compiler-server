package com.compiler.server.executor

import com.compiler.server.executor.JavaStreamingExecutor.Companion.PipeResult.LIMIT_EXCEEDED
import com.compiler.server.executor.JavaStreamingExecutor.Companion.PipeResult.NORMAL
import com.compiler.server.streaming.ServerStreamingOutputMapper
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Integer.min
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


@Component
class JavaStreamingExecutor {

  private val streamingOutputMapper = ServerStreamingOutputMapper()

  companion object {
    private const val MAX_OUTPUT_SIZE = 100 * 1024
    private const val EXECUTION_TIMEOUT = 20000L
    private const val STREAMING_PIPE_BUFFER_SIZE = 1024

    private enum class PipeResult {
      NORMAL,
      LIMIT_EXCEEDED
    }
  }

  fun execute(args: List<String>, output: OutputStream) {
    Runtime.getRuntime().exec(args.toTypedArray()).apply {
      try {
        outputStream.close()
        val standardOutput = streamingPipe(inputStream, output, MAX_OUTPUT_SIZE)

        val currTime = System.currentTimeMillis()
        val futuresList = Executors.newSingleThreadExecutor()
          .invokeAll(listOf(standardOutput), EXECUTION_TIMEOUT, TimeUnit.MILLISECONDS)

        val outputFuture = futuresList.first()

        when {
          outputFuture.isCancelled -> {
            output.write(streamingOutputMapper.writeStderrAsBytes(ExecutorMessages.TIMEOUT_MESSAGE))
          }
          outputFuture.get(0, TimeUnit.MILLISECONDS) == LIMIT_EXCEEDED -> {
            output.write(streamingOutputMapper.writeStderrAsBytes(ExecutorMessages.TOO_LONG_OUTPUT_MESSAGE))
          }
        }
      } catch (any: Exception) {
        output.write(streamingOutputMapper.writeThrowableAsBytes(any))
      } finally {
        try {
          // don't need this process any more. It will not allow to close IO handlers if not destroyed or finished
          this.destroy()
          inputStream.close()
          errorStream.close()
        } catch (_: IOException) {
          // don't care
        }
      }
    }
  }

  private fun streamingPipe(input: InputStream, output: OutputStream, limit: Int): Callable<PipeResult> = Callable {
    var limitExceeded = false
    try {
      var bytesCopied = 0
      val buffer = ByteArray(STREAMING_PIPE_BUFFER_SIZE)
      var bytes = input.read(buffer)
      while (bytes >= 0 && bytesCopied < limit) {
        val bytesToCopy = min(bytes, limit - bytesCopied)
        if (bytesToCopy < bytes) {
          limitExceeded = true
        }
        output.write(buffer, 0, bytesToCopy)
        output.flush()
        bytesCopied += bytesToCopy
        bytes = input.read(buffer)
      }
    } catch (_: Exception) {
      // something happened with the stream. Just return what we've collected so far
    }
    if (limitExceeded) LIMIT_EXCEEDED else NORMAL
  }
}