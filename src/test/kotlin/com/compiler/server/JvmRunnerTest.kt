package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.model.JvmExecutionResult
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmRunnerTest : BaseExecutorTest() {

  @Test
  fun `base execute test JVM`() {
    val executionResult = run(
      code = "fun main() {\n println(\"Hello, world!!!\")\n}",
      contains = "Hello, world!!!",
      addByteCode = false,
    )
    assertNull((executionResult as JvmExecutionResult).jvmByteCode, "Bytecode should not be generated")
  }

  @Test
  fun `jvm bytecode`() {
    val executionResult = run(
      code = "fun main() {\n println(\"Hello, world!!!\")\n}",
      contains = "Hello, world!!!",
      addByteCode = true,
    )
    
    val byteCode = (executionResult as JvmExecutionResult).jvmByteCode!!
    assertContains(byteCode, "public static synthetic main([Ljava/lang/String;)V", message = byteCode)
    assertContains(byteCode, "public final static main()V", message = byteCode)
    assertContains(byteCode, "LDC \"Hello, world!!!\"", message = byteCode)
    assertContains(byteCode, "INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/Object;)V", message = byteCode)
  }

  @Test
  fun `execute test JVM different package`() {
    run(
      code = "package com.example\nfun main() {\n println(\"Hello, world!!!\")\n}",
      contains = "Hello, world!!!"
    )
  }

  @Test
  fun `no main class jvm test`() {
    runWithException(
      code = "fun main1() {\n    println(\"sdf\")\n}",
      contains = "IllegalArgumentException",
      message = "No main method found in project",
    )
  }

  @Test
  fun `multiple main classes jvm test`() {
    runWithException(
      code = """
                fun main() {
                    println("sdf")
                }
                class A {
                    companion object {
                        @JvmStatic
                        fun main(x: Array<String>) {
                        }
                    }
                }""".trimIndent(),
      contains = "IllegalArgumentException",
      message = "Multiple classes in project contain main methods found: FileKt, A",
    )
  }

  @Test
  fun `base execute test JVM multi`() {
    run(
      code = listOf(
        "import cat.Cat\n\nfun main(args: Array<String>) {\nval cat = Cat(\"Kitty\")\nprintln(cat.name)\n}",
        "package cat\n    class Cat(val name: String)"
      ),
      contains = "Kitty"
    )
  }

  @Test
  fun `correct kotlin version test jvm`() {
    run(
      code = "fun main() {\n    println(KotlinVersion?.CURRENT)\n}",
      contains = version().substringBefore("-")
    )
  }

  @Test
  fun `report deprecation warning`() {
    val result = run(
      contains = "<outStream>97\n</outStream>", /*language=kotlin */ code = """
        fun main() {
          println("abc"[0].toInt())
        }
      """.trimIndent()
    )

    assertEquals(1, result.compilerDiagnostics.size)
    assertEquals(1, result.compilerDiagnostics[0].interval?.start?.line)
    assertEquals(19, result.compilerDiagnostics[0].interval?.start?.ch)
    assertEquals(
      "'toInt(): Int' is deprecated. Conversion of Char to Number is deprecated. Use Char.code property instead.",
      result.compilerDiagnostics[0].message
    )
  }
}
