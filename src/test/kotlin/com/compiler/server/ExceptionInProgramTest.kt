package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.TestCompiler
import org.junit.jupiter.api.Assertions

class ExceptionInProgramTest : BaseExecutorTest() {
  @TestCompiler
  fun `command line index of bound jvm`() {
    runWithException(
      code = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[2])\n}",
      contains = "java.lang.ArrayIndexOutOfBoundsException"
    )
  }

  @TestCompiler
  fun `security read file`() {
    runWithException(
      code = "import java.io.*\n\nfun main() {\n    val f = File(\"executor.policy\")\n    print(f.toURL())\n}",
      contains = "java.security.AccessControlException"
    )
  }

  @TestCompiler
  fun `security connection exception`() {
    runWithException(
      code = "import java.net.*\n\nfun main() {\n    val connection = URL(\"http://www.android.com/\").openConnection() as HttpURLConnection\n\tval d = connection.inputStream.bufferedReader().readText()\n\tprint(d)\n}",
      contains = "java.security.AccessControlException"
    )
  }

  @TestCompiler
  fun `kotlin npe`() {
    runWithException(
      code = """
        fun main() {
          val s: String? = null
          print(s!!)
          }
      """.trimIndent(),
      contains = "kotlin.KotlinNullPointerException"
    )
  }

  @TestCompiler
  fun `kotlin out of memory in executor`() {
    val result = runWithException(
      code = """
        import java.io.*
        fun main() {
          val out = PrintStream(ByteArrayOutputStream())
          while(true) { out.print("alex") }
        }
      """.trimIndent(),
      contains = "java.lang.OutOfMemoryError"
    )
    print(result)
    Assertions.assertTrue(result.getException()?.message == "Java heap space")
  }

  @TestCompiler
  fun `kotlin compiler crash`() {
    val result = runWithException(
      code = "fun main(args: Array<String>) {\n    println(\"Hello, world!\")\n    \n    fun factorial(x: Int): Int{\n        var fact = 1\n        for(i in 2..x){\n            fact*=i\n        }\n        return fact\n    }\n    \n    fun catalogue(type: String): (arg: Int) -> Int{\n        return when(type){\n            \"double\" -> {x: Int -> 2*x}\n            \"square\" -> {x: Int -> x*x}\n            \"factorial\" -> ::factorial\n            else -> {x: Int -> x}\n        }\n    }\n    \n    val x = 10\n    println(\"The double of \$x is \${catalogue(\"double\")(x)}\")\n    println(\"The square of \$x is \${catalogue(\"square\")(x)}\")\n    println(\"The factorial of \$x is \${catalogue(\"factorial\")(x)}\")\n\n}",
      contains = "org.jetbrains.kotlin.util.KotlinFrontEndException"
    )
    Assertions.assertTrue(result.getException()?.stackTrace?.size!! <= 3)
  }

}