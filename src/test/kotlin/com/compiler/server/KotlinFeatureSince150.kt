package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.assertNoErrors
import org.junit.jupiter.api.Test

class KotlinFeatureSince150 : BaseExecutorTest() {

  @Test
  fun `KT-45991 on JVM IR BE`() {
    run(
      code = """
            fun ex1_(res: Result<Int>) {
                res.fold(
                    onSuccess = { println("Ex " + it) },
                    onFailure = {},
                )
            }
            
            fun ex1(res: Result<Int>) {
                res.fold(
                    onSuccess = { ex1_(res) },
                    onFailure = { ex1_(Result.failure(it)) }
                )
            }
            
            val ex2_: (Result<Int>) -> Unit = { res ->
                res.fold(
                    onSuccess = { println("Ex " + it) },
                    onFailure = {},
                )
            }
            
            val ex2: (Result<Int>) -> Unit = { res ->
                res.fold(
                    onSuccess = { ex2_(Result.success(it)) },
                    onFailure = { ex2_(Result.failure(it)) }
                )
            }
            
            
            val ex3_: (Result<Int>) -> Unit = { res ->
                res.fold(
                    onSuccess = { println("Ex " + it) },
                    onFailure = {},
                )
            }
            
            val ex3: (Result<Int>) -> Unit = { res ->
                res.fold(
                    onSuccess = { ex3_(res) },
                    onFailure = { ex3_(Result.failure(it)) }
                )
            }
            
            fun main() {
                ex1(Result.success(1)) // works
                ex2(Result.success(2)) // works
                ex3(Result.success(3)) // doesn't work
            }
                """.trimIndent(),
      contains = "Ex 1\n" +
        "Ex 2\n" +
        "Ex 3"
    ).assertNoErrors()
  }

  @Test
  fun `unsigned integer types`() {
    run(
      code = """
        fun main(){
            val zero=0U // Define unsigned numbers with literal suffixes
            val ten=10.toUInt() // or by converting non-negative signed numbers
            //val minusOne: UInt = -1U // Error: unary minus is not defined
            val range: UIntRange=zero..ten// Separate types for ranges and progressions
            
            for (i in range) print(i)
            println()
            println("UInt covers the range from ${"$"}{UInt.MIN_VALUE} to ${"$"}{UInt.MAX_VALUE}")
        }
         """.trimIndent(),
      contains = "012345678910\nUInt covers the range from 0 to 4294967295"
    ).assertNoErrors()
  }

  @Test
  fun `API for uppercase`() {
    run(
      code = """
          fun main() {
            println("Kotlin".uppercase()) 
          }
        """.trimIndent(),
      contains = "KOTLIN"
    )
  }

  @Test
  fun `char to int conversions`() {
    run(
      code = """
          fun main() {
            println("4".toInt()) // returns 4
            println('4'.toInt()) // returns 52
          }
        """.trimIndent(),
      contains = "4\n52"
    )
  }

  @Test
  fun `char to digit conversions`() {
    run(
      code = """
          fun main() {
            val capsK=Char(75) // ‘K’
            val one='1'.digitToInt(10) // 1
            val digitC=12.digitToChar(16) // hexadecimal digit ‘C’
            
            println("${"$"}{capsK}otlin ${"$"}{one}.5.0-R${"$"}{digitC}") // “Kotlin 1.5.0-RC”
            println(capsK.code) // 75
          }
        """.trimIndent(),
      contains = "Kotlin 1.5.0-RC\n75"
    )
  }

  @Test
  fun `Char category new API`() {
    run(
      code = """
          fun main() {
            val array="Kotlin 1.5.0-RC".toCharArray()
            val (letterOrDigit, punctuation) = array.partition { it.isLetterOrDigit() }
            val (upperCase, notUpperCase ) =array.partition { it.isUpperCase() }
            
            println("${"$"}letterOrDigit, ${"$"}punctuation") 
            println("${"$"}upperCase, ${"$"}notUpperCase") 
            
            if (array[0].isDefined()) println(array[0].category)
          }
        """.trimIndent(),
      contains = "[K, o, t, l, i, n, 1, 5, 0, R, C], [ , ., ., -]\n" +
        "[K, R, C], [o, t, l, i, n,  , 1, ., 5, ., 0, -]\n" +
        "UPPERCASE_LETTER"
    )
  }

  @Test
  fun `string true toBooleanStrict`() {
    run(
      code = """
          fun main() {
             println("true".toBooleanStrict()) // True
          }
        """.trimIndent(),
      contains = "true"
    )
  }

  @Test
  fun `int toBooleanStrict`() {
    runWithException(
      code = """
          fun main() {
             println("1".toBooleanStrict())
          }
        """.trimIndent(),
      contains = "java.lang.IllegalArgumentException"
    )
  }


  @Test
  fun `int toBooleanStrictOrNull`() {
    run(
      code = """
          fun main() {
              println("1".toBooleanStrictOrNull()) // null
          }
        """.trimIndent(),
      contains = "null"
    )
  }

  @Test
  fun `String True toBooleanStrictOrNull`() {
    run(
      code = """
          fun main() {
              println("True".toBooleanStrictOrNull())
          }
        """.trimIndent(),
      contains = "null"
    )
  }

  @Test
  fun `floored division on integers`() {
    run(
      code = """
          fun main() {
                println("Floored division -5/3: ${"$"}{(-5).floorDiv(3)}")
          }
        """.trimIndent(),
      contains = "Floored division -5/3: -2"
    )
  }


  @Test
  fun `Inline classes are Stable`() {
    run(
      code = """
          @JvmInline
          value class Hours(val value: Int)

          fun main(args: Array<String>) {
          	val hours = Hours(12)
              println(hours.value)
          }
        """.trimIndent(),
      contains = "12"
    )
  }
}