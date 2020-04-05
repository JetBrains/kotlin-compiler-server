package com.compiler.server

import com.compiler.server.base.BaseJUnitTest
import com.compiler.server.base.ExecutorMode
import com.compiler.server.base.StreamingResult
import com.compiler.server.base.SynchronousResult
import com.compiler.server.executor.ExecutorMessages
import com.compiler.server.generator.ErrStreamChunk
import com.compiler.server.model.TestStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource


class JUnitTestsRunnerTest : BaseJUnitTest() {

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `interrupt after a lot of text test`(mode: ExecutorMode) {
    val test = testRaw(
      mode,
      "fun start(): String {\n    repeat(100009){\n        print(\"alex\")\n    }\n    \n    return \"\"\n}",
      "import org.junit.Assert\nimport org.junit.Test\n\nclass TestStart {\n    @Test fun testOk() {\n        Assert.assertEquals(\"OK\", start())\n    }\n}",
      koansUtilsFile
    )
    val message = when (test) {
      is SynchronousResult -> test.result.text
      is StreamingResult -> test.result.filterIsInstance(ErrStreamChunk::class.java)
          .joinToString(separator = "") { it.errStream }
    }
    Assertions.assertTrue(message.contains(ExecutorMessages.TOO_LONG_OUTPUT_MESSAGE), "Actual: $message, Excepted: ${ExecutorMessages.TOO_LONG_OUTPUT_MESSAGE} ")

  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `base fail junit test`(mode: ExecutorMode) {
    val test = test(
      mode,
      "fun start(): String = \"OP\"",
      "import org.junit.Assert\nimport org.junit.Test\n\nclass TestStart {\n    @Test fun testOk() {\n        Assert.assertEquals(\"OK\", start())\n    }\n}",
      koansUtilsFile
    )
    val fail = test.first()
    Assertions.assertTrue(fail.status == TestStatus.FAIL)
    Assertions.assertNotNull(fail.comparisonFailure, "comparisonFailure should not be a null")
    fail.comparisonFailure?.let {
      Assertions.assertTrue(it.actual == "OP")
      Assertions.assertTrue(it.expected == "OK")
    }
  }
}