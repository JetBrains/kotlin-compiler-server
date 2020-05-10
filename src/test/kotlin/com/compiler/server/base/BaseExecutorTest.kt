package com.compiler.server.base

import com.compiler.server.generator.TestProjectRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BaseExecutorTest : BaseTest() {
  @Autowired
  lateinit var testRunner: TestProjectRunner

  fun complete(
    code: String,
    line: Int,
    character: Int,
    completions: List<String>,
    isJs: Boolean = false
  ) = testRunner.complete(code, line, character, completions, isJs)

  fun highlight(code: String) = testRunner.highlight(code)

  fun highlightJS(code: String) = testRunner.highlightJS(code)

  fun run(code: String, contains: String, args: String = ""): RawExecutionResult = when (mode) {
    ExecutorMode.SYNCHRONOUS -> SynchronousResult(testRunner.run(code, contains, args))
    ExecutorMode.STREAMING -> StreamingResult(testRunner.runStreaming(code, contains, args))
  }

  fun run(code: List<String>, contains: String) = when (mode) {
    ExecutorMode.SYNCHRONOUS -> testRunner.multiRun(code, contains)
    ExecutorMode.STREAMING -> testRunner.multiRunStreaming(code, contains)
  }

  fun runJs(code: String, contains: String, args: String = "") = testRunner.runJs(code, contains, args)

  fun runJs(code: List<String>, contains: String) = testRunner.multiRunJs(code, contains)

  fun translateToJs(code: String) = testRunner.translateToJs(code)

  fun runWithException(code: String, contains: String): RawExecutionResult = when (mode) {
    ExecutorMode.SYNCHRONOUS -> SynchronousResult(testRunner.runWithException(code, contains))
    ExecutorMode.STREAMING -> StreamingResult(testRunner.runWithExceptionStreaming(code, contains))
  }

  fun version() = testRunner.getVersion()
}