package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.ExecutorMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class CommandLineArgumentsTest : BaseExecutorTest() {

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `command line arguments jvm test`(mode: ExecutorMode) {
    run(
      mode = mode,
      code = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[1])\n}",
      args = "0 1",
      contains = "0\n1\n"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `command line string arguments jvm test`(mode: ExecutorMode) {
    run(
      mode = mode,
      code = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[1])\n}",
      args = "alex1 alex2",
      contains = "alex1\nalex2\n"
    )
  }

  @Test
  fun `command line arguments js test`() {
    runJs(
      code = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[1])\n}",
      args = "0 1",
      contains = "main(['0', '1']);"
    )
  }

  @Test
  fun `command line string arguments js test`() {
    runJs(
      code = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[1])\n}",
      args = "alex1 alex2",
      contains = "main(['alex1', 'alex2']);"
    )
  }

}