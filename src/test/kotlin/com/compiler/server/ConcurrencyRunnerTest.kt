package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ConcurrencyRunnerTest : BaseExecutorTest() {
  @Test
  fun `a lot of hello word test JVM`() {
    runManyTest {
      run(
        code = "fun main() {\n println(\"Hello, world!!!\")\n}",
        contains = "Hello, world!!!"
      )
    }
  }

  @Test
  fun `a lot of complete test`() {
    runManyTest {
      complete(
        code = "fun main() {\n    val alex = 1\n    val alex1 = 1 + a\n}",
        line = 2,
        character = 21,
        completions = listOf(
          "alex"
        )
      )
    }
  }

  @Test
  fun `a lot of complete test JS`() {
    runManyTest {
      complete(
        code = "fun main() {\n    val alex = 1\n    val alex1 = 1 + a\n}",
        line = 2,
        character = 21,
        completions = listOf(
          "alex"
        ),
        isJs = true
      )
    }
  }

  @Test
  fun `a lot of hello word test JS`() {
    runManyTest {
      runJsIr(
        code = "fun main() {\n println(\"Hello, world!!!\")\n}",
        contains = "println('Hello, world!!!');"
      )
    }
  }

  @Test
  fun `a lot of hello word test JS IR`() {
    runManyTest {
      runJsIr(
        code = "fun main() {\n println(\"Hello, world!!!\")\n}",
        contains = "println('Hello, world!!!');"
      )
    }
  }

  private fun runManyTest(times: Int = 100, test: () -> Unit) {
    runBlocking {
      launch(Dispatchers.IO) {
        for (i in 0 until times) {
          launch(Dispatchers.IO) {
            test()
          }
        }
      }.join()
    }
  }

}