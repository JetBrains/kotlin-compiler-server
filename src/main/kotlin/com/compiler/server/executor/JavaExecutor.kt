package com.compiler.server.executor

import com.compiler.server.model.ExceptionDescriptor
import com.compiler.server.model.JavaExecutionResult
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

  // TODO:: move them into application properties
  companion object {
    const val MAX_OUTPUT_SIZE = 100 * 1024
    const val EXECUTION_TIMEOUT = 10000L
  }

  data class ProgramOutput(
    val standardOutput: String,
    val errorOutput: String,
    val exception: Exception? = null
  ) {
    constructor(exception: Exception) : this("", "", exception)

    fun asExecutionResult(): JavaExecutionResult {
      return JavaExecutionResult(
        text = "<outStream>$standardOutput\n</outStream><errStream>$errorOutput</errStream>",
        exception = exception?.let {
          ExceptionDescriptor(it.message ?: "no message", it::class.java.toString())
        })
    }
  }

  fun execute(args: List<String>): JavaExecutionResult {
    return Runtime.getRuntime().exec(args.toTypedArray()).use {
      // why?
      outputStream.close()

      val standardOut = InputStreamReader(this.inputStream).buffered()
      val standardError = InputStreamReader(this.errorStream).buffered()

      val standardOutput = asyncBufferedOutput(standardOut, limit = MAX_OUTPUT_SIZE)
      val errorOutput = asyncBufferedOutput(standardError, limit = MAX_OUTPUT_SIZE)

      try {
        val futuresList = Executors.newFixedThreadPool(2) // one thread per output
                .invokeAll(listOf(standardOutput, errorOutput), EXECUTION_TIMEOUT, TimeUnit.MILLISECONDS)
        // we do not wait for process to end, while either time-limit or output-limit triggered
        // program itself will be destroyed right after we'll leave this method

        val outputResults = futuresList.map {
          try {
            it.get()
          } catch (_: Exception) {
            ""
          }
        }

        val (standardText, errorText) = outputResults
        val exception = if (errorText.isNotEmpty()) Exception(errorText) else null

        when {
            futuresList.any { !it.isDone } -> {
              // execution timeout. Both Future objects must be in 'done' state, to say that process finished
              ProgramOutput(
                      ExecutorMessages.TIMEOUT_MESSAGE,
                      errorText,
                      exception
              ).asExecutionResult()
            }
            outputResults.any {it.length >= MAX_OUTPUT_SIZE} -> {
              // log-limit exceeded
              ProgramOutput(
                      ExecutorMessages.TOO_LONG_OUTPUT_MESSAGE,
                      errorText,
                      exception
              ).asExecutionResult()
            }
            else -> {
              // normal exit
              ProgramOutput(standardText, errorText, exception).asExecutionResult()
            }
        }
      } catch (any: Exception) {
        // all sort of things may happen, so we better be aware
        any.printStackTrace()
        ProgramOutput(any).asExecutionResult()
      }
      finally {
        try {
          standardOut.close()
          standardError.close()
        }
        catch (_: IOException) {
          // don't care
        }
      }
    }
  }

  private fun asyncBufferedOutput(standardOut: BufferedReader, limit: Int): Callable<String> = Callable {
    val output = StringBuilder()
    try {
      while (output.length < limit) {
        val line = standardOut.readLine()
        if (line == null) break
        else output.append(line)
      }
    } catch (_: Exception) {
      // something happened with the stream. Just return what we've collected so far
    }

    output.toString()
  }


  private fun <T> Process.use(body: Process.() -> T) = try {
    body()
  }
  finally {
    destroy()
  }

}

class JavaArgumentsBuilder(
  val classPaths: String,
  val mainClass: String,
  val policy: Path,
  val memoryLimit: Int,
  val args: String
) {
  fun toArguments(): List<String> {
    return listOf(
      "java",
      "-Djava.security.manager",
      "-Xmx" + memoryLimit + "M",
      "-Djava.security.policy=$policy",
      "-classpath"
    ) + classPaths + mainClass + args.split(" ")
  }
}