package com.compiler.server.executor

import com.compiler.server.model.ProgramOutput
import com.compiler.server.utils.escapeString
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Component
class JavaExecutor {

  companion object {
    const val MAX_OUTPUT_SIZE = 100 * 1024
    const val EXECUTION_TIMEOUT = 10000L
  }

  fun execute(args: List<String>): ProgramOutput {
    return Runtime.getRuntime().exec(args.toTypedArray()).use {
      outputStream.close()

      val standardOut = InputStreamReader(this.inputStream).buffered()
      val standardError = InputStreamReader(this.errorStream).buffered()

      val standardOutput = asyncBufferedOutput(standardOut, limit = MAX_OUTPUT_SIZE)
      val errorOutput = asyncBufferedOutput(standardError, limit = MAX_OUTPUT_SIZE)

      try {
        val currTime = System.currentTimeMillis()
        val futuresList = Executors.newFixedThreadPool(2) // one thread per output
          .invokeAll(listOf(standardOutput, errorOutput), EXECUTION_TIMEOUT, TimeUnit.MILLISECONDS)
        // we do not wait for process to end, while either time-limit or output-limit triggered
        // program itself will be destroyed right after we'll leave this method
        println("Script execution time in millis: " + (System.currentTimeMillis() - currTime))

        val outputResults = futuresList.map {
          try {
            it.get()
          } catch (_: Exception) {
            ""
          }
        }

        val (standardText, _) = outputResults

        when {
          futuresList.any { it.isCancelled } -> {
            // execution timeout. Both Future objects must be in 'done' state, to say that process finished
            ProgramOutput(restriction = ExecutorMessages.TIMEOUT_MESSAGE)
          }

          outputResults.any { it.length >= MAX_OUTPUT_SIZE } -> {
            // log-limit exceeded
            ProgramOutput(restriction = ExecutorMessages.TOO_LONG_OUTPUT_MESSAGE)
          }

          else -> {
            // normal exit
            ProgramOutput(standardText)
          }
        }
      } catch (any: Exception) {
        // all sort of things may happen, so we better be aware
        ProgramOutput(exception = any)
      } finally {
        try {
          // don't need this process any more. It will not allow to close IO handlers if not destroyed or finished
          this.destroy()
          standardOut.close()
          standardError.close()
        } catch (_: IOException) {
          // don't care
        }
      }
    }
  }

  private fun asyncBufferedOutput(standardOut: BufferedReader, limit: Int): Callable<String> = Callable {
    val output = StringBuilder()
    try {
      while (output.length < limit) {
        val line = standardOut.readLine() ?: break
        output.appendLine(escapeString(line))
      }
    } catch (_: Exception) {
      // something happened with the stream. Just return what we've collected so far
    }

    output.toString()
  }


  private fun <T> Process.use(body: Process.() -> T) = try {
    body()
  } finally {
    destroy()
  }

}

class CommandLineArgument(
  val classPaths: String,
  val mainClass: String?,
  val policy: Path,
  val memoryLimit: Int,
  val arguments: List<String>
) {
  fun toList(): List<String> {
    return (listOf(
      "java",
      "-Xmx" + memoryLimit + "M",
      "-Djava.security.manager",
      "-Djava.security.policy=$policy",
      "-ea",
      "-classpath"
    ) + classPaths + mainClass + arguments).filterNotNull()
  }
}