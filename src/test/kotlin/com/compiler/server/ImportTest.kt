package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class ImportTest : BaseExecutorTest() {
  @Test
  fun `import class`() {
    completeWithImport(
      code = "fun main() {\n    val rand = Random\n}",
      line = 1,
      character = 21,
      completions = listOf(
        Pair("Random", "kotlin.random.Random")
      )
    )
  }

  @Test
  fun `import class with parameters`() {
    completeWithImport(
      code = """fun main() {
        |    randVal = Random(3)
        |    printn(randomVal.nextInt())
        |}
      """.trimMargin(),
      line = 1,
      character = 20,
      completions = listOf(
        Pair("Random", "kotlin.random.Random")
      )
    )
  }

  @Test
  fun `import by prefix`() {
    completeWithImport(
      code = "fun main() {\n" +
        "    val t = Thread\n" +
        "}",
      line = 1,
      character = 18,
      completions = listOf(
        Pair("ThreadSafe", "org.junit.runner.notification.RunListener.ThreadSafe"),
        Pair("ThreadSafeHeapNode", "kotlinx.coroutines.internal.ThreadSafeHeapNode"),
        Pair("ThreadSafeHeap", "kotlinx.coroutines.internal.ThreadSafeHeap"),
        Pair("ThreadContextElement", "kotlinx.coroutines.ThreadContextElement"),
        Pair("ThreadLocal", "kotlin.native.concurrent.ThreadLocal")
      )
    )
  }

/*
  ToDo: import methods
  example:
  "import kotlin.math.PI\n" +
        "\n" +
        "fun main() {\n" +
        "    val pi = PI\n" +
        "    println(pi)\n" +
        "    val alex = 1\n" +
        "    val alex1 = 1 + a\n" +
        "}"
 */
}