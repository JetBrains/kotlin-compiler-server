package com.compiler.server

import com.compiler.server.base.BaseTestClass
import com.compiler.server.generator.TestProjectRunner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CompletionTest : BaseTestClass() {
  @Autowired
  private lateinit var testRunner: TestProjectRunner

  @Test
  fun `variable completion test`() {
    testRunner.complete(
      code = "fun main() {\n    val alex = 1\n    val alex1 = 1 + a\n}",
      line = 2,
      character = 21,
      completions = listOf(
        "alex"
      )
    )
  }

  @Test
  fun `variable completion test js`() {
    testRunner.complete(
      code = "fun main() {\n    val alex = 1\n    val alex1 = 1 + a\n}",
      line = 2,
      character = 21,
      completions = listOf(
        "alex"
      ),
      isJs = true
    )
  }

  @Test
  fun `double to int completion test`() {
    testRunner.complete(
      code = "fun main() {\n    3.0.toIn\n}",
      line = 1,
      character = 12,
      completions = listOf(
        "toInt()"
      )
    )
  }

  @Test
  fun `double to int completion test js`() {
    testRunner.complete(
      code = "fun main() {\n    3.0.toIn\n}",
      line = 1,
      character = 12,
      completions = listOf(
        "toInt()"
      ),
      isJs = true
    )
  }


  @Test
  fun `listOf completion test`() {
    testRunner.complete(
      code = "fun main() {\n    list\n}",
      line = 1,
      character = 8,
      completions = listOf(
        "listOf()",
        "listOf(T)",
        "listOfNotNull(T?)",
        "listOfNotNull(vararg T?)",
        "listOf(vararg T)"
      )
    )
  }

  @Test
  fun `listOf completion test js`() {
    testRunner.complete(
      code = "fun main() {\n    list\n}",
      line = 1,
      character = 8,
      completions = listOf(
        "listOf()",
        "listOf(T)",
        "listOfNotNull(T?)",
        "listOfNotNull(vararg T?)",
        "listOf(vararg T)"
      ),
      isJs = true
    )
  }
}
