package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class ConvertToWasmRunnerTest : BaseExecutorTest() {
  @Test
  fun `base execute test`() {
    runWasm(
      code = "fun main() {\n println(\"Hello, world!!!\")\n}",
      contains = "Hello, world!!!"
    )
  }
}