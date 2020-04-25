package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class KotlinFeatureSince1370 : BaseExecutorTest() {

  @Test
  fun `kotlin blog post 1370 feature 1`() {
    run(
      code = """
        @OptIn(ExperimentalStdlibApi::class)
        fun main() {
            val needsZero = true
            val initial = listOf(2, 6, 41)
            val ints = buildList { // this: MutableList
                if (needsZero) {
                    add(0)
                }
                initial.mapTo(this) { it + 1 }
            }
            println(ints) // [0, 3, 7, 42]
        }
      """.trimIndent(),
      contains = "[0, 3, 7, 42]"
    )
  }

  @Test
  fun `kotlin blog post 1370 feature 2`() {
    run(
      code = """
        import kotlin.reflect.cast
        
        @OptIn(ExperimentalStdlibApi::class)
        fun main() {
            val kClass = String::class
            println(kClass.simpleName) // String
            println(kClass.qualifiedName) // kotlin.String
        
            println(kClass.isInstance("abc")) // true
            println(kClass.isInstance(10)) // false
            println(kClass.cast("abc")) // abc
        }
      """.trimIndent(),
      contains = "String\nkotlin.String\ntrue\nfalse\nabc"
    )
  }

  @Test
  fun `kotlin blog post 1370 feature 3`() {
    run(
      code = """
        @OptIn(ExperimentalStdlibApi::class)
        fun main() {
            val deque = ArrayDeque(listOf(1, 2, 3))
        
            deque.addFirst(0)
            deque.addLast(4)
            println(deque) // [0, 1, 2, 3, 4]
        
            println(deque.first()) // 0
            println(deque.last()) // 4
        
            deque.removeFirst()
            deque.removeLast()
            println(deque) // [1, 2, 3]
        }
      """.trimIndent(),
      contains = "[0, 1, 2, 3, 4]\n0\n4\n[1, 2, 3]"
    )
  }

  @Test
  fun `kotlin blog post 1370 feature 4`() {
    run(
      code = """
        @OptIn(ExperimentalStdlibApi::class)
        fun main() {
            val list = listOf(1, 2, 3)
            println(list.randomOrNull()) // 2
            println(list.reduceOrNull { a, b -> a + b }) // 6
        
            val emptyList = emptyList<Int>()
            println(emptyList.randomOrNull()) // null
            println(emptyList.reduceOrNull { a, b -> a + b }) // null
        }
      """.trimIndent(),
      contains = "6\nnull\nnull"
    )
  }

  @Test
  fun `kotlin blog post 1370 feature 5`() {
    run(
      code = """
        @OptIn(ExperimentalStdlibApi::class)
        fun main() {
            val ints = (1..4).asSequence()
            println(ints.fold(0) { acc, elem -> acc + elem }) // 10
        
            val sequence = ints.scan(0) { acc, elem -> acc + elem }
            println(sequence.toList()) // [0, 1, 3, 6, 10]
        }
      """.trimIndent(),
      contains = "10\n[0, 1, 3, 6, 10]"
    )
  }


}
