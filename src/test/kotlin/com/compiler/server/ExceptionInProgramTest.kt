package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.ExecutorMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ExceptionInProgramTest : BaseExecutorTest() {
  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `command line index of bound jvm`(mode: ExecutorMode) {
    runWithException(
      mode = mode,
      code = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[2])\n}",
      contains = "java.lang.ArrayIndexOutOfBoundsException"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `security read file`(mode: ExecutorMode) {
    runWithException(
      mode = mode,
      code = "import java.io.*\n\nfun main() {\n    val f = File(\"executor.policy\")\n    print(f.toURL())\n}",
      contains = "java.security.AccessControlException"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `security connection exception`(mode: ExecutorMode) {
    runWithException(
      mode = mode,
      code = "import java.net.*\n\nfun main() {\n    val connection = URL(\"http://www.android.com/\").openConnection() as HttpURLConnection\n\tval d = connection.inputStream.bufferedReader().readText()\n\tprint(d)\n}",
      contains = "java.security.AccessControlException"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `kotlin npe`(mode: ExecutorMode) {
    runWithException(
      mode = mode,
      code = """
        fun main() {
          val s: String? = null
          print(s!!)
          }
      """.trimIndent(),
      contains = "kotlin.KotlinNullPointerException"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `kotlin out of memory in executor`(mode: ExecutorMode) {
    val result = runWithException(
      mode = mode,
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

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `kotlin compiler crash`(mode: ExecutorMode) {
    val result = runWithException(
      mode = mode,
      code = "fun main(args: Array<String>) {\n    println(\"Hello, world!\")\n    \n    fun factorial(x: Int): Int{\n        var fact = 1\n        for(i in 2..x){\n            fact*=i\n        }\n        return fact\n    }\n    \n    fun catalogue(type: String): (arg: Int) -> Int{\n        return when(type){\n            \"double\" -> {x: Int -> 2*x}\n            \"square\" -> {x: Int -> x*x}\n            \"factorial\" -> ::factorial\n            else -> {x: Int -> x}\n        }\n    }\n    \n    val x = 10\n    println(\"The double of \$x is \${catalogue(\"double\")(x)}\")\n    println(\"The square of \$x is \${catalogue(\"square\")(x)}\")\n    println(\"The factorial of \$x is \${catalogue(\"factorial\")(x)}\")\n\n}",
      contains = "org.jetbrains.kotlin.util.KotlinFrontEndException"
    )
    Assertions.assertTrue(result.getException()?.stackTrace?.size!! <= 3)
  }

}