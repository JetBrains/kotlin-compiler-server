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

  @Test
  fun `blog stdlib 14M2 appendLine`() {
    run(
      code = """
          fun main() {
          //sampleStart
            println(buildString {
              appendLine("Hello,")
              appendLine("Kotlin 1.4-M2")
            })
        //sampleEnd
        }
        """.trimIndent(),
      contains = "Hello,\nKotlin 1.4-M2"
    )
  }

  @Test
  fun `blog stdlib 14M2 arrays oneach shuffle`() {
    run(
      code = """
         fun main() {
            //sampleStart
                var language = ""
                val letters = arrayOf("k", "o", "t", "l", "i", "n")
                val fileExt = letters.onEach { language += it }
                   .filterNot { it in "aeuio" }.take(2)
                   .joinToString(prefix = ".", separator = "")
                println(language) // "kotlin"
                println(fileExt) // ".kt"
            
                letters.shuffle()
                println(letters.contentToString())
            //sampleEnd
            }
        """.trimIndent(),
      contains = "kotlin\n.kt"
    )
  }

  @Test
  fun `blog stdlib 14M2 arrays sort`() {
    run(
      code = """
         fun main() {
        //sampleStart
            val letters = arrayOf("i", "o", "k", "l", "t", "n")
            letters.reverse(0, 3)
            letters.sortDescending(2, 5)
            println(letters.contentToString()) // [k, o, t, l, i, n]
        //sampleEnd
        }
        """.trimIndent(),
      contains = "[k, o, t, l, i, n]"
    )
  }

  @Test
  fun `blog stdlib 14M2 setOfNotNull`() {
    run(
      code = """
        fun main() {
        //sampleStart
            val set = setOfNotNull(null, 1, 2, 0, null)
            println(set)
        //sampleEnd
        }
        """.trimIndent(),
      contains = "[1, 2, 0]"
    )
  }

  @Test
  fun `blog stdlib 14M2 indexed`() {
    run(
      code = """
        fun main() {
        //sampleStart
            val list = mutableListOf("a", "b", "c").onEachIndexed {
                index, item -> println(index.toString() + ":" + item)
            }
            val emptyList = emptyList<Int>()
            emptyList.reduceIndexedOrNull {index, a, b -> index + a + b} // null
        //sampleEnd
        }
        """.trimIndent(),
      contains = "0:a\n1:b\n2:c"
    )
  }

  @Test
  fun `blog stdlib 14M2 runningFold Reduce`() {
    run(
      code = """
        fun main() {
        //sampleStart
            val numbers = mutableListOf(0, 1, 2, 3, 4, 5)
            val runningReduceSum = numbers.runningReduce() { sum, item -> sum + item} // [0, 1, 3, 6, 10, 15]
            val runningFoldSum = numbers.runningFold(10) { sum, item -> sum + item} // [10, 10, 11, 13, 16, 20, 25]
        //sampleEnd
        }
        """.trimIndent(),
      contains = ""
    ).assertNoErrors()
  }

  @Test
  fun `blog stdlib 14M2 accept null`() {
    run(
      code = """
        fun main() {
        //sampleStart
           val s: String? = null
           println(s.toBoolean())  // false
        //sampleEnd
        }
        """.trimIndent(),
      contains = "false"
    )
  }

  @Test
  fun `blog stdlib 14M2 double float constants`() {
    run(
      code = """
        fun main() {
        //sampleStart
            println(Double.NaN)  // NaN
            println(Double.NEGATIVE_INFINITY)  // -Infinity
            println(Double.MAX_VALUE < Double.POSITIVE_INFINITY)  // true
            println(Double.SIZE_BITS) // 64
        //sampleEnd
        }
        """.trimIndent(),
      contains = "NaN\n-Infinity\ntrue\n64"
    )
  }

  @Test
  fun `blog stdlib 14M2 maxOf vararg`() {
    run(
      code = """
        fun main() {
        //sampleStart
            val max = maxOf(1, 2, 3, 4)
            println(max)
        //sampleEnd
        }
        """.trimIndent(),
      contains = "4"
    )
  }

  @Test
  fun `blog stdlib 14M2 delegate`() {
    run(
      code = """
        class MyClass {
           var newName: Int = 0
           @Deprecated("Use 'newName' instead", ReplaceWith("newName"))
           var oldName: Int by this::newName
        }
        
        fun main() {
           val myClass = MyClass()
           // Notification: 'oldName: Int' is deprecated.
           // Use 'newName' instead
           myClass.oldName = 42
           println(myClass.newName) // 42
        }
        """.trimIndent(),
      contains = "42"
    )
  }
}