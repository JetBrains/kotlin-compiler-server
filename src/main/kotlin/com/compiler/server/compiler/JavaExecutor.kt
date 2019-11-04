package com.compiler.server.compiler

import com.compiler.server.compiler.model.JavaExecutionResult
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.util.Timer
import java.util.TimerTask

object JavaExecutor {

    data class StackTraceElement(val className: String, val methodName: String, val fileName: String, val lineNumber: Int)
    data class ExceptionDescriptor(val message: String, val fullName: String, val stackTrace: List<StackTraceElement> = emptyList(), val cause: ExceptionDescriptor? = null)

    data class ProgramOutput(val standardOutput: String, val errorOutput: String, val exception: Exception? = null) {
        fun asExecutionResult(): JavaExecutionResult {
            return JavaExecutionResult(text = "<outStream>$standardOutput\n</outStream>", exception = exception?.let {
                ExceptionDescriptor(it.message
                        ?: "no message", it::class.java.toString())
            })
        }
    }

    fun execute(args: List<String>): JavaExecutionResult {
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
            interruptAfter(60000, this, listOf(standardThread, errorThread))
            try {
                waitFor()
                standardThread.join(10000)
                errorThread.join(10000)
            } finally {
                try {
                    standardOut.close()
                    standardError.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            val exception = if (errorText.toString().isNotEmpty()) {
                println(errorText.toString())
                Exception(errorText.toString())
            } else null
            ProgramOutput(standardText.toString(), errorText.toString(), exception).asExecutionResult()
        }
    }

    private fun <T> Process.use(body: Process.() -> T) = try { body() } finally { destroy() }

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
                string.appendln(line)
            }
        } catch (e: Throwable) {
            if (!Thread.interrupted()) {
                e.printStackTrace()
            }
        }
    }
}