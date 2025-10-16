package com.compiler.server.base

import com.compiler.server.generator.TestProjectRunner
import org.intellij.lang.annotations.Language
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BaseExecutorTest {
  @Autowired
  private lateinit var testRunner: TestProjectRunner

  fun highlight(@Language("kotlin") code: String) = testRunner.highlight(code)

  fun highlightJS(@Language("kotlin") code: String) = testRunner.highlightJS(code)

  fun highlightWasm(@Language("kotlin") code: String) = testRunner.highlightWasm(code)

  fun run(@Language("kotlin") code: String, contains: String, args: String = "", addByteCode: Boolean = false) = testRunner.run(code, contains, args, addByteCode)

  fun run(code: List<String>, contains: String, addByteCode: Boolean = false) = testRunner.multiRun(code, contains, addByteCode)

  fun runJsIr(
    @Language("kotlin")
    code: String,
    contains: String,
    args: String = ""
  ) = testRunner.runJs(code, contains, args) { project ->
    convertToJsIr(
      project,
    )
  }

  fun runJsIr(
    code: List<String>,
    contains: String
  ) = testRunner.multiRunJs(code, contains) { project ->
    convertToJsIr(
      project,
    )
  }

  fun runWasm(
    @Language("kotlin")
    code: String,
    contains: String
  ) = testRunner.runWasm(code, contains)

  fun translateToJsIr(@Language("kotlin") code: String) = testRunner.translateToJsIr(code)

  fun runWithException(@Language("kotlin") code: String, contains: String, message: String? = null, addByteCode: Boolean = false) =
    testRunner.runWithException(code, contains, message, addByteCode)

  fun version() = testRunner.getVersion()
}
