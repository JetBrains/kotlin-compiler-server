package com.compiler.server

import com.compiler.server.base.BaseJUnitTest
import com.compiler.server.executor.ExecutorMessages
import com.compiler.server.model.TestStatus
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertNull

class JUnitTestsRunnerTest : BaseJUnitTest() {

  @Test
  fun `interrupt after a lot of text test`() {
    val test = testRaw(
      "fun start(): String {\n    repeat(100009){\n        print(\"alex\")\n    }\n    \n    return \"\"\n}",
      "import org.junit.Assert\nimport org.junit.Test\n\nclass TestStart {\n    @Test fun testOk() {\n        Assert.assertEquals(\"OK\", start())\n    }\n}",
      koansUtilsFile
    )
    val message = test?.text
    Assertions.assertTrue(
      message?.contains(ExecutorMessages.TOO_LONG_OUTPUT_MESSAGE) == true,
      "Actual: $message, Excepted: ${ExecutorMessages.TOO_LONG_OUTPUT_MESSAGE} "
    )

  }

  @Test
  fun `base fail junit test`() {
    @Language("kotlin")
    val testCode = """
      import org.junit.Assert
      import org.junit.Test
      
      class TestStart {
          @Test fun testOk() {
              Assert.assertEquals("OK", start())
          }
      }
    """.trimIndent()
    val sourceCode = """fun start(): String = "OP""""
    
    val test = test(sourceCode, testCode, koansUtilsFile)
    val fail = test.first()
    Assertions.assertTrue(fail.status == TestStatus.FAIL)
    Assertions.assertNotNull(fail.comparisonFailure, "comparisonFailure should not be a null")
    fail.comparisonFailure?.let {
      Assertions.assertTrue(it.actual == "OP")
      Assertions.assertTrue(it.expected == "OK")
    }
  }
  
  @Test
  fun `no bytecode`() {
    @Language("kotlin")
    val testCode = """
      import org.junit.Assert
      import org.junit.Test
      
      class TestStart {
          @Test fun testOk() {
              Assert.assertEquals("OK", "OK")
          }
      }
    """.trimIndent()
    val testResults = testRaw(testCode, addByteCode = false)
    assertNull(testResults!!.jvmByteCode, "Bytecode should not be generated")
  }
  
  @Test
  fun `with bytecode`() {
    @Language("kotlin")
    val testCode = """
      import org.junit.Assert
      import org.junit.Test
      
      class TestStart {
          @Test fun testOk() {
              Assert.assertEquals("OK", "OK")
          }
      }
    """.trimIndent()
    val testResults = testRaw(testCode, addByteCode = true)
    val byteCode = testResults!!.jvmByteCode!!
    assertContains(byteCode, "public final testOk()V")
  }
}
