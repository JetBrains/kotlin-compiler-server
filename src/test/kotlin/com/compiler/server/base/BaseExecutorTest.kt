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
    args: String = "",
    useIrCompiler: Boolean
  ) = testRunner.runJs(code, contains, args, useIrCompiler)

  fun runJs(
    code: List<String>,
    contains: String,
    useIrCompiler: Boolean
  ) = testRunner.multiRunJs(code, contains, useIrCompiler)

  fun translateToJs(code: String) = testRunner.translateToJs(code)

  fun runWithException(code: String, contains: String) = testRunner.runWithException(code, contains)

  fun version() = testRunner.getVersion()
}