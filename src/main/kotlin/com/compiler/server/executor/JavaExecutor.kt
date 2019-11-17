package com.compiler.server.executor

import com.compiler.server.model.ExceptionDescriptor
import com.compiler.server.model.JavaExecutionResult
import com.compiler.server.utils.escapeString
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

@Component
class JavaExecutor {

  data class ProgramOutput(
    val standardOutput: String,
    val errorOutput: String,
    val exception: Exception? = null
  ) {
    fun asExecutionResult(): JavaExecutionResult {
      return JavaExecutionResult(
        text = "<outStream>$standardOutput\n</outStream>",
        exception = exception?.let {
          ExceptionDescriptor(it.message ?: "no message", it::class.java.toString())
        })
    }
  }

  fun execute(args: List<String>): JavaExecutionResult {
    return Runtime.getRuntime().exec(args.toTypedArray()).use {
      outputStream.close()
      val interruptCondition = ProcessInterruptCondition()
      val standardOut = InputStreamReader(this.inputStream).buffered()
      val standardError = InputStreamReader(this.errorStream).buffered()
      val errorText = StringBuilder()
      val standardText = StringBuilder()
      val standardThread = appendTo(standardText, standardOut, interruptCondition)
      standardThread.start()
      val errorThread = appendTo(errorText, standardError, interruptCondition)
      errorThread.start()

      val interruptMsg = Executors.newSingleThreadExecutor().submit(
        interruptAfter(
          delay = 10000,
          process = this,
          threads = listOf(standardThread, errorThread),
          interruptCondition = interruptCondition
        )
      )

      var message: String
      try {
        waitFor()
        interruptCondition.exitNow()
        message = interruptMsg.get()
        standardThread.join(10000)
        errorThread.join(10000)
      }
      finally {
        try {
          standardOut.close()
          standardError.close()
        }
        catch (e: IOException) {
          e.printStackTrace()
        }
      }
      val exception = if (errorText.toString().isNotEmpty()) {
        Exception(errorText.toString())
      }
      else null
      if (message.isEmpty()) {
        ProgramOutput(standardText.toString(), errorText.toString(), exception).asExecutionResult()
      }
      else ProgramOutput(message, errorText.toString(), exception).asExecutionResult()
    }
  }

  private fun <T> Process.use(body: Process.() -> T) = try {
    body()
  }
  finally {
    destroy()
  }

  private fun interruptAfter(
    delay: Long,
    process: Process,
    threads: List<Thread>,
    interruptCondition: ProcessInterruptCondition
  ): Callable<String> = Callable {
    val result = when (interruptCondition.waitForCondition(delay)) {
      ProcessInterruptCondition.ConditionType.LOG_SIZE -> "evaluation stopped while log size exceeded max size"
      ProcessInterruptCondition.ConditionType.TIMEOUT -> "evaluation stopped while it's taking too long"
      else -> ""
    }
    threads.forEach { it.interrupt() }
    process.destroy()
    result
  }

  private fun appendTo(
    string: StringBuilder,
    from: BufferedReader,
    interruptCondition: ProcessInterruptCondition
  ) = Thread {
    try {
      while (true) {
        val line = from.readLine() as String?
        if (Thread.interrupted() || line == null) break
        interruptCondition.appendCharacterCounter(line.length)
        string.appendln(escapeString(line))
      }
    }
    catch (e: Throwable) {
      if (!Thread.interrupted()) {
        e.printStackTrace()
      }
    }
  }
}

class ProcessInterruptCondition {
  private lateinit var conditionBreak: ConditionType
  private var totalCharactersOutput: Int = 0
  private val lock = ReentrantLock()
  private val condition = lock.newCondition()

  enum class ConditionType {
    TIMEOUT,
    LOG_SIZE,
    NORMAL
  }

  fun waitForCondition(delay: Long): ConditionType = withLock {
    if (!condition.await(delay, TimeUnit.MILLISECONDS))
      return@withLock ConditionType.TIMEOUT
    return@withLock conditionBreak
  }

  fun appendCharacterCounter(length: Int) = withLock {
    this.totalCharactersOutput += length
    if (totalCharactersOutput > 10) {
      this.conditionBreak = ConditionType.LOG_SIZE
      condition.signal()
    }
  }

  fun exitNow() = withLock {
    this.conditionBreak = ConditionType.NORMAL
    condition.signal()
  }

  private fun <T> withLock(block: () -> T): T {
    lock.lock()
    try {
      return block()
    }
    finally {
      lock.unlock()
    }
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