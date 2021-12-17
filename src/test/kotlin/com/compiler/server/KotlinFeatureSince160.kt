package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class KotlinFeatureSince160 : BaseExecutorTest() {

    @Test
    fun `Stable Duration API`() {
        run(
            code = """
            import kotlin.time.Duration.Companion.seconds
                
            fun main() {
            // sampleStart
             val duration = 5000.seconds
             println("There are ${"$"}{duration.inWholeMinutes} minutes in ${"$"}{duration.inWholeHours} hours")
            // sampleEnd
            }
      """.trimIndent(),
            contains = "There are 83 minutes in 1 hours"
        )
    }

    @Test
    fun `Stable bit rotation operations for integers`() {
        run(
            code = """
          fun main() {
        //sampleStart
        val number: Short = 0b10001
        println(number.rotateRight(2).toString(radix = 2)) // 100000000000100
        println(number.rotateLeft(2).toString(radix = 2))  // 1000100
        //sampleEnd
}
      """.trimIndent(),
            contains = "100000000000100\n1000100"
        )
    }

    @Test
    fun `Stable Regex function for splitting a string to a sequence`() {
        run(
            code = """
      fun main(){
      //sampleStart
      val colorsText = "green, red , brown&blue, orange, pink&green"
      val regex = "[,\\s]+".toRegex()
      val mixedColor = regex.splitToSequence(colorsText)
      .firstOrNull { it.contains('&') }
      println(mixedColor) // "brown&blue"
      //sampleEnd
      }
      """.trimIndent(),
            contains = "<outStream>brown&amp;blue\n</outStream>"
        )
    }

}


