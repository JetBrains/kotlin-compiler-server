package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class DatetimeRunnerTest : BaseExecutorTest() {
    @Test
    fun `kotlinx datetime basic test`() {
        run(
            code = """
            import kotlinx.datetime.*

            fun main() {
              val zone = TimeZone.of("Europe/Berlin")
              val today = Clock.System.todayIn(zone)
              println(today.year in 2000..3000)
            }
      """.trimIndent(),
            contains = "true"
        )
    }

}