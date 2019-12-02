package com.compiler.server

import com.compiler.server.generator.TestProjectRunner
import com.compiler.server.model.TestDescription
import com.compiler.server.model.TestStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class JUnitTestsRunnerTest {
  @Autowired
  private lateinit var testRunner: TestProjectRunner

  @Test
  fun `base junit test`() {
    val test = testRunner.test(
      "fun start(): String = \"OK\"",
      "import org.junit.Assert\nimport org.junit.Test\n\nclass TestStart {\n    @Test fun testOk() {\n        Assert.assertEquals(\"OK\", start())\n    }\n}",
      koansUtilsFile
    )
    successTestCheck(test)
  }

  @Test
  fun `base fail junit test`() {
    val test = testRunner.test(
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


  fun successTestCheck(testResults: List<TestDescription>) {
    testResults
      .map { it.status }
      .forEach {
        Assertions.assertTrue(it == TestStatus.OK)
      }
  }


  private val koansUtilsFile = "package koans.util\n\nfun String.toMessage() = \"The function '\$this' is implemented incorrectly\"\n\nfun String.toMessageInEquals() = toMessage().inEquals()\n\nfun String.inEquals() = this"
}