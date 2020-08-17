package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
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
  fun `import class with parameters`() {
    complete(
      code = """fun main() {
        |    randVal = Random(3)
        |    printn(randomVal.nextInt())
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
}