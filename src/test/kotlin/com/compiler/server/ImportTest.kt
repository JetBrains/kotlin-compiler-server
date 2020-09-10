package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import common.model.Completion
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ImportTest : BaseExecutorTest() {
  @Test
  fun `import class`() {
    complete(
      code = "fun main() {\n    val rand = Random\n}",
      line = 1,
      character = 21,
      completions = listOf(
        "Random  (kotlin.random.Random)"
      )
    )
  }

  @Test
  fun `import class with import`() {
    val foundCompletions = getCompletions(
      code = "import kotlin.math.sin\nimport java.util.Random\nfun main() {\n" +
        "    val rand = Random\n" +
        "}",
      line = 3,
      character = 21
    )
    completionContainsCheckOtherImports(
      foundCompletions = foundCompletions,
      completions = listOf(
        Pair("Random  (kotlin.random.Random)", true)
      )
    )
  }

  @Test
  fun `import method with other import`() {
    val foundCompletions = getCompletions(
      code = "import kotlin.math.sin\nfun main() {\n" +
        "    val s = sin\n" +
        "}",
      line = 2,
      character = 15
    ).map { it.displayText }
    val completions = listOf(
      "sin(x: Double)  (kotlin.math.sin)",
      "sin(x: Float)  (kotlin.math.sin)"
    )
    completions.forEach {
      Assertions.assertFalse(foundCompletions.contains(it))
    }
  }

  @Test
  fun `import class with parameters`() {
    complete(
      code = """fun main() {
        |    randVal = Random(3)
        |    println(randomVal.nextInt())
        |}
      """.trimMargin(),
      line = 1,
      character = 20,
      completions = listOf(
        "Random  (kotlin.random.Random)"
      )
    )
  }

  @Test
  fun `import method`() {
    complete(
      code = "fun main() {\n" +
        "    val s = sin\n" +
        "}",
      line = 1,
      character = 15,
      completions = listOf(
        "sin(x: Double)  (kotlin.math.sin)",
        "sin(x: Float)  (kotlin.math.sin)"
      )
    )
  }

  private fun completionContainsCheckOtherImports(
    foundCompletions: List<Completion>,
    completions: List<Pair<String, Boolean>>
  ) {
    val result = foundCompletions.map { Pair(it.displayText, it.hasOtherImports) }
    Assertions.assertTrue(result.isNotEmpty())
    completions.forEach { suggest ->
      Assertions.assertTrue(result.contains(suggest))
    }
  }
}