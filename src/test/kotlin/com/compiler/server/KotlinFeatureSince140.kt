package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.assertNoErrors
import org.junit.jupiter.api.Test

class KotlinFeatureSince140 : BaseExecutorTest() {
  @Test
  fun `fun interface works in 140`() {
    run(
      code = """
        fun interface Action {
            fun run()
        }
        
        fun runAction(a: Action) = a.run()
        
        fun main() {
            runAction {
                println("Hello, Kotlin 1.4!")
            }
        }
                """.trimIndent(),
      contains = "Hello, Kotlin 1.4!"
    ).assertNoErrors()
  }

  @Test
  fun `new inference works in 140`() {
    highlight(
      code = """
        val rulesMap: Map<String, (String?) -> Boolean> = mapOf(
            "weak" to { it != null },
            "medium" to { !it.isNullOrBlank() },
            "strong" to { it != null && "^[a-zA-Z0-9]+${'$'}".toRegex().matches(it) }
        )
                """.trimIndent()
    ).assertNoErrors()
  }

  @Test
  fun `isInstance in 140`() {
    run(
      code = """
        import kotlin.reflect.*
        fun main() {
          val a = String::class.isInstance("")
          val b = String::class.isInstance(42)
          println(a)
          println(b)
        }
      """.trimIndent(),
      contains = "true\nfalse"
    )
  }

  @Test
  fun `cast safeCast in 140`() {
    run(
      code = """
        import kotlin.reflect.*
        fun main() {
          val a = String::class.safeCast("f")
          print(a == "f")
        }
      """.trimIndent(),
      contains = "true"
    )
  }

  @Test
  fun `simpleName qualifiedName in 140`() {
    run(
      code = """
        import kotlin.reflect.*
        fun main() {
          println("kotlin.String" == String::class.qualifiedName)
          println("Number" == Number::class.simpleName)
        }
      """.trimIndent(),
      contains = "true\ntrue"
    )
  }
}