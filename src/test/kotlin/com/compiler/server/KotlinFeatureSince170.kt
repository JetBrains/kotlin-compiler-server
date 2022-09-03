package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class KotlinFeatureSince170 : BaseExecutorTest() {

  @Test
  fun `maxOrNull for empty list returns null`() {
    run(
      code = """
            fun main() {
            val numbers = listOf<Int>()
            println(numbers.maxOrNull()) // "null"
            }
      """.trimIndent(),
      contains = "null"
    )
  }

  @Test
  fun `matching regex with matchesAt()`() {
    run(
      code = """
            fun main() {
            val releaseText = "Kotlin 1.7.0 is on its way!"
            // regular expression: one digit, dot, one digit, dot, one or more digits
            val versionRegex = "\\d[.]\\d[.]\\d+".toRegex()

            println(versionRegex.matchesAt(releaseText, 0)) // "false"
            }
      """.trimIndent(),
      contains = "false"
    )
  }

  @Test
  fun `matching regex with matchAt()`() {
    run(
      code = """
            fun main(){
            val releaseText = "Kotlin 1.7.0 is on its way!"
            val versionRegex = "\\d[.]\\d[.]\\d+".toRegex()
            println(versionRegex.matchAt(releaseText, 7)?.value) // "1.7.0"
            }
      """.trimIndent(),
      contains = "1.7.0"
    )
  }

  @Test
  fun `named backreferencing`() {
    run(
      code = """
            fun main(){
            val regex = "(?<title>\\w+), yes \\k<title>".toRegex()
            val match = regex.find("Do you copy? Sir, yes Sir!")!!
            println(match.value) // "Sir, yes Sir"
            }
      """.trimIndent(),
      contains = "Sir, yes Sir"
    )
  }

  @Test
  fun `named groups in replacement expressions`() {
    run(
      code = """
            fun main(){
            val dateRegex = Regex("(?<dd>\\d{2})-(?<mm>\\d{2})-(?<yyyy>\\d{4})")
            val input = "Date of birth: 27-04-2022"
            println(dateRegex.replace(input, "\${'$'}{yyyy}-\${'$'}{mm}-\${'$'}{dd}")) // "Date of birth: 2022-04-27"  — by name
            }
      """.trimIndent(),
      contains = "Date of birth: 2022-04-27"
    )
  }

  @Test
  fun `New operator for defining the open-ended range`() {
    run(
      code = """
        fun main() {
          val number = 0.25
          
          when (number) {
            in 0.0..<0.25 -> println("first quarter")
            in 0.25..<0.5 -> println("second quarter")
            in 0.5..<0.75 -> println("third quarter")
            in 0.75..1.0 ->  println("last quarter")
          }
      }
      """.trimIndent(),
    contains = "second quarter"
    )
  }
}