package com.compiler.server

import com.compiler.server.base.BaseJUnitTest
import com.compiler.server.model.TestStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CoroutinesTestRunnerTest : BaseJUnitTest() {
  @Test
  fun `coroutines-test test`() {
    val test = test("""
import org.junit.Assert.*		
import org.junit.Test
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay

internal class SampleTest {
    suspend fun fetchData(): String {
        delay(1000L)
        return "Hello world"
    }

    @Test
    fun dataShouldBeHelloWorld() = runTest {
        val data = fetchData()
        assertEquals("Hello world", data)
    }
}
      """.trimIndent()
    )

    Assertions.assertTrue(test.first().status == TestStatus.OK)
  }
}