package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ExceptionInProgramTest : BaseExecutorTest() {
  @Test
  fun `command line index of bound jvm`() {
    runWithException(
      code = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[2])\n}",
      contains = "java.lang.ArrayIndexOutOfBoundsException"
    )
  }

  @Test
  fun `kotlin npe`() {
    runWithException(
      code = """
        fun main() {
          val s: String? = null
          print(s!!)
          }
      """.trimIndent(),
      contains = "java.lang.NullPointerException"
    )
  }

  @Test
  fun `kotlin out of memory in executor`() {
    val result = runWithException(
      code = """
        fun main() {
         List(100_000) { ByteArray(1000_000_000) }
        }
      """.trimIndent(),
      contains = "java.lang.OutOfMemoryError"
    )
    print(result)
    Assertions.assertTrue(result.exception?.message == "Java heap space")
  }

  @Test
  fun `assert exception`() {
    runWithException(
      """
        class Test() {
          init {
            assert(false)
          }
        }

        fun main() {
          Test()
        }
      """.trimIndent(),
      contains = "java.lang.AssertionError"
    )
  }

  @Test
  fun `validate kotlin-test available`() {
    runWithException(
      code = "import kotlin.test.assertTrue\n\nfun main(args: Array<String>) { assertTrue(false) }",
      contains = "java.lang.AssertionError"
    )
  }
}
