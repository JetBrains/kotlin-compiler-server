package com.compiler.server.base

import com.compiler.server.generator.TestProjectRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BaseExecutorTest {
  @Autowired
  private lateinit var testRunner: TestProjectRunner

  fun complete(
    code: String,
    line: Int,
    character: Int,
    completions: List<String>,
    isJs: Boolean = false
  ) = testRunner.complete(code, line, character, completions, isJs)

  fun getCompletions(
    code: String,
    line: Int,
    character: Int,
    isJs: Boolean = false
  ) = testRunner.getCompletions(code, line, character, isJs)

  fun highlight(code: String) = testRunner.highlight(code)

  fun highlightJS(code: String) = testRunner.highlightJS(code)

  fun run(code: String, contains: String, args: String = "") = testRunner.run(code, contains, args)

  fun run(code: List<String>, contains: String) = testRunner.multiRun(code, contains)

  fun runJs(
    code: String,
    contains: String,
    args: String = ""
  ) = testRunner.runJs(code, contains, args) { project ->
    convertToJs(project)
  }

  fun runJs(
    code: List<String>,
    contains: String
  ) = testRunner.multiRunJs(code, contains) { project ->
    convertToJs(project)
  }

  fun runJsIr(
    code: String,
    contains: String,
    args: String = ""
  ) = testRunner.runJs(code, contains, args) { project ->
    convertToJsIr(project)
  }

  fun runJsIr(
    code: List<String>,
    contains: String
  ) = testRunner.multiRunJs(code, contains) { project ->
    convertToJsIr(project)
  }

  fun runWasm(
    code: String,
    contains: String
  ) = testRunner.runWasm(code, contains)

  fun translateToJs(code: String) = testRunner.translateToJs(code)

  fun runWithException(code: String, contains: String) = testRunner.runWithException(code, contains)

  fun version() = testRunner.getVersion()
}