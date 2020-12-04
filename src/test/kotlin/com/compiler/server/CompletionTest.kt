package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class CompletionTest : BaseExecutorTest() {
  @Test
  fun `variable completion test`() {
    complete(
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
    complete(
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
    complete(
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
    complete(
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
    complete(
      code = "fun main() {\n    list\n}",
      line = 1,
      character = 8,
      completions = listOf(
        "listOf()",
        "listOf(element: T)",
        "listOfNotNull(element: T?)",
        "listOfNotNull(vararg elements: T?)",
        "listOf(vararg elements: T)"
      )
    )
  }

  @Test
  fun `listOf completion test js`() {
    complete(
      code = "fun main() {\n    list\n}",
      line = 1,
      character = 8,
      completions = listOf(
        "listOf()",
        "listOf(element: T)",
        "listOfNotNull(element: T?)",
        "listOfNotNull(vararg elements: T?)",
        "listOf(vararg elements: T)"
      ),
      isJs = true
    )
  }
}
