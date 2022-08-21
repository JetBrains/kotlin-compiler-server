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
  fun `security read file`() {
    runWithException(
      code = "import java.io.*\n\nfun main() {\n    val f = File(\"executor.policy\")\n    print(f.toURL())\n}",
      contains = "java.security.AccessControlException"
    )
  }

  @Test
  fun `security connection exception`() {
    runWithException(
      code = "import java.net.*\n\nfun main() {\n    val connection = URL(\"http://www.android.com/\").openConnection() as HttpURLConnection\n\tval d = connection.inputStream.bufferedReader().readText()\n\tprint(d)\n}",
      contains = "java.security.AccessControlException"
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
