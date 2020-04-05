package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.ExecutorMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ConcurrencyRunnerTest : BaseExecutorTest() {
  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  @Disabled
  fun `a lot of hello word test JVM`(mode: ExecutorMode) {
    runManyTest {
      run(
        mode = mode,
        code = "fun main() {\n println(\"Hello, world!!!\")\n}",
        contains = "Hello, world!!!"
      )
    }
  }

  @Test
  @Disabled
  fun `a lot of hello word test JS`() {
    runManyTest {
      runJs(
        code = "fun main() {\n println(\"Hello, world!!!\")\n}",
        contains = "println('Hello, world!!!');"
      )
    }
  }

  private fun runManyTest(times: Int = 100, test: () -> Unit) {
    runBlocking {
      GlobalScope.launch(Dispatchers.IO) {
        for (i in 0 until times) {
          launch {
            test()
          }
        }
      }.join()
    }
  }

}