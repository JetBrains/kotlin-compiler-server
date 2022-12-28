package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class KotlinFeatureSince180 : BaseExecutorTest() {

    @Test
    fun `stable cbrt() function`() {
        run(
            code = """
            import kotlin.math.*

            fun main() {
            val num = 27
            val negNum = -num

            println("The cube root of ${"$"}{num.toDouble()} is: " + cbrt(num.toDouble()))
            }
      """.trimIndent(),
            contains = "The cube root of 27.0 is: 3.0"
        )
    }

    @Test
    fun `stable cbrt() function for negative values`() {
        run(
            code = """
            import kotlin.math.*

            fun main() {
            val num = 27
            val negNum = -num

            println("The cube root of ${"$"}{negNum.toDouble()} is: " + cbrt(negNum.toDouble()))
            }
      """.trimIndent(),
            contains = "The cube root of -27.0 is: -3.0"
        )
    }

    @Test
    fun `calculate the time difference between multiple TimeMarks`() {
        run(
            code = """
           import kotlin.time.*

            @OptIn(ExperimentalTime::class)
            fun main() {
            val timeSource = TimeSource.Monotonic
            val mark1 = timeSource.markNow()
            Thread.sleep(500) // Sleep 0.5 seconds.
            val mark2 = timeSource.markNow()
    
            repeat(4) { n ->
            val mark3 = timeSource.markNow()
            val elapsed1 = mark3 - mark1
            val elapsed2 = mark3 - mark2

            println(mark2 > mark1)
            }
        }
      """.trimIndent(),
            contains = "true"
        )
    }
}

