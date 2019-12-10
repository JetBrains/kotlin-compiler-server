package com.compiler.server.executor

import com.compiler.server.model.JunitExecutionResult
import com.compiler.server.model.TestDescription
import com.compiler.server.utils.escapeString
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

@Component
class JUnitExecutor {

  fun execute(args: List<String>): JunitExecutionResult {
    return Runtime.getRuntime().exec(args.toTypedArray()).use {
      outputStream.close()
      val standardOut = InputStreamReader(this.inputStream).buffered()
      val standardError = InputStreamReader(this.errorStream).buffered()
      val errorText = StringBuilder()
      val standardText = StringBuilder()
      val standardThread = appendTo(standardText, standardOut)
      standardThread.start()
      val errorThread = appendTo(errorText, standardError)
      errorThread.start()
      interruptAfter(
        delay = 10000,
        process = this,
        threads = listOf(standardThread, errorThread)
      )
      try {
        waitFor()
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
      val result = jacksonObjectMapper().readValue(
        standardText.toString(),
        object : TypeReference<Map<String, List<TestDescription>>>() {}
      )
      JunitExecutionResult(result)
    }
  }

  private fun <T> Process.use(body: Process.() -> T) = try {
    body()
  }
  finally {
    destroy()
  }

  private fun interruptAfter(delay: Long, process: Process, threads: List<Thread>) {
    Timer(true).schedule(object : TimerTask() {
      override fun run() {
        threads.forEach { it.interrupt() }
        process.destroy()
      }
    }, delay)
  }

  private fun appendTo(string: StringBuilder, from: BufferedReader) = Thread {
    try {
      while (true) {
        val line = from.readLine()
        if (Thread.interrupted() || line == null) break
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