package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import model.Completion
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
      Assertions.assertFalse(
        foundCompletions.contains(it),
        "Suggests adding an import, even though it has already been added."
      )
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

  @Test
  fun `open bracket after import completion`() {
    val foundCompletionsTexts = getCompletions(
      code = "fun main() {\n" +
        "    val s = sin\n" +
        "}",
      line = 1,
      character = 15
    ).map { it.text }
    val completions = listOf("kotlin.math.sin(")
    completions.forEach {
      Assertions.assertTrue(
        foundCompletionsTexts.contains(it),
        "Wrong completion text for import. Expected to find $it in $foundCompletionsTexts"
      )
    }
  }

  @Test
  fun `brackets after import completion`() {
    val foundCompletionsTexts = getCompletions(
      code = "fun main() {\n" +
        "    val timeZone  = getDefaultTimeZone\n" +
        "}",
      line = 1,
      character = 38
    ).map { it.text }
    val completions = listOf(
      "com.fasterxml.jackson.databind.util.StdDateFormat.getDefaultTimeZone()"
    )
    completions.forEach {
      Assertions.assertTrue(
        foundCompletionsTexts.contains(it),
        "Wrong completion text for import. Expected to find $it in $foundCompletionsTexts"
      )
    }
  }

  @Test
  fun `import class js`() {
    complete(
      code = "fun main() {\n    val rand = Random\n}",
      line = 1,
      character = 21,
      completions = listOf(
        "Random  (kotlin.random.Random)"
      ),
      isJs = true
    )
  }

  @Test
  fun `import method with other import js`() {
    val foundCompletions = getCompletions(
      code = "import kotlin.math.sin\nfun main() {\n" +
        "    val s = sin\n" +
        "}",
      line = 2,
      character = 15,
      isJs = true
    ).map { it.displayText }
    val completions = listOf(
      "sin(x: Double)  (kotlin.math.sin)",
      "sin(x: Float)  (kotlin.math.sin)"
    )
    completions.forEach {
      Assertions.assertFalse(
        foundCompletions.contains(it),
        "Suggests adding an import, even though it has already been added."
      )
    }
  }

  @Test
  fun `import class with parameters js`() {
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
      ),
      isJs = true
    )
  }

  @Test
  fun `import method js`() {
    complete(
      code = "fun main() {\n" +
        "    val s = sin\n" +
        "}",
      line = 1,
      character = 15,
      completions = listOf(
        "sin(x: Double)  (kotlin.math.sin)",
        "sin(x: Float)  (kotlin.math.sin)"
      ),
      isJs = true
    )
  }

  @Test
  fun `open bracket after import completion js`() {
    val foundCompletionsTexts = getCompletions(
      code = "fun main() {\n" +
        "    val s = sin\n" +
        "}",
      line = 1,
      character = 15,
      isJs = true
    ).map { it.text }
    val completions = listOf("kotlin.math.sin(")
    completions.forEach {
      Assertions.assertTrue(
        foundCompletionsTexts.contains(it),
        "Wrong completion text for import. Expected to find $it in $foundCompletionsTexts"
      )
    }
  }

  @Test
  fun `not jvm imports in js imports`() {
    val foundCompletionsTexts = getCompletions(
      code = "fun main() {\n" +
        "    val timeZone  = getDefaultTimeZone\n" +
        "}",
      line = 1,
      character = 38,
      isJs = true
    ).map { it.text }
    val completions = listOf(
      "com.fasterxml.jackson.databind.util.StdDateFormat.getDefaultTimeZone()"
    )
    completions.forEach {
      Assertions.assertFalse(
        foundCompletionsTexts.contains(it),
        "Wrong completion text for import. Expected not to find $it in $foundCompletionsTexts"
      )
    }
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