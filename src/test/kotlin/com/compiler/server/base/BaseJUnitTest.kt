package com.compiler.server.base

import com.compiler.server.generator.TestProjectRunner
import com.compiler.server.model.TestDescription
import com.compiler.server.model.TestStatus
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BaseJUnitTest {

  @Autowired
  private lateinit var testRunner: TestProjectRunner

  fun test(@Language("kotlin") vararg test: String, addByteCode: Boolean = false) = testRunner.test(*test, addByteCode = addByteCode)

  fun testRaw(@Language("kotlin") vararg test: String, addByteCode: Boolean = false) = testRunner.testRaw(*test, addByteCode = addByteCode)

  fun runKoanTest(@Language("kotlin") vararg testFile: String, addByteCode: Boolean = false) {
    val test = test(
      *testFile,
      koansUtilsFile,
      addByteCode = addByteCode,
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