package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.TestCompiler
import com.compiler.server.executor.ExecutorMessages

class InterruptExecutionTest : BaseExecutorTest() {

  @TestCompiler
  fun `interrupt after 10 sec test`() {
    run(
      code = "fun main() {\n    try {\n      Thread.sleep(110000L)\n      println(\"Hello\")\n    } catch(e: Exception){\n        print(\"ups\")\n    }\n}",
      contains = ExecutorMessages.TIMEOUT_MESSAGE
    )
  }

  @TestCompiler
  fun `interrupt after a lot of text test`() {
    run(
      code = "fun main() {\n    for (i in 1..1025){\n        repeat(25) {print(\"Alex\")}\n println()\n    }\n}",
      contains = ExecutorMessages.TOO_LONG_OUTPUT_MESSAGE
    )
  }
}