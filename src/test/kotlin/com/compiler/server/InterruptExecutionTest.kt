package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.ExecutorMode
import com.compiler.server.executor.ExecutorMessages
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class InterruptExecutionTest : BaseExecutorTest() {

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `interrupt after 10 sec test`(mode: ExecutorMode) {
    run(
      mode = mode,
      code = "fun main() {\n    try {\n      Thread.sleep(110000L)\n      println(\"Hello\")\n    } catch(e: Exception){\n        print(\"ups\")\n    }\n}",
      contains = ExecutorMessages.TIMEOUT_MESSAGE
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `interrupt after a lot of text test`(mode: ExecutorMode) {
    run(
      mode = mode,
      code = "fun main() {\n    for (i in 1..1025){\n        repeat(25) {print(\"Alex\")}\n println()\n    }\n}",
      contains = ExecutorMessages.TOO_LONG_OUTPUT_MESSAGE
    )
  }
}