package com.compiler.server.base

import com.compiler.server.generator.TestProjectRunner
import com.compiler.server.model.TestDescription
import com.compiler.server.model.TestStatus
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BaseJUnitTest {

  @Autowired
  private lateinit var testRunner: TestProjectRunner

  fun test(vararg test: String) = testRunner.test(*test)

  fun testRaw(vararg test: String) = testRunner.testRaw(*test)

  fun runKoanTest(vararg testFile: String) {
    val test = test(
      *testFile,
      koansUtilsFile
    )
    successTestCheck(test)
  }

  private fun successTestCheck(testResults: List<TestDescription>) {
    testResults
      .map { it.status }
      .forEach {
        Assertions.assertTrue(it == TestStatus.OK)
      }
  }

  val koansUtilsFile =
    "package koans.util\n\nfun String.toMessage() = \"The function '\$this' is implemented incorrectly\"\n\nfun String.toMessageInEquals() = toMessage().inEquals()\n\nfun String.inEquals() = this"
}