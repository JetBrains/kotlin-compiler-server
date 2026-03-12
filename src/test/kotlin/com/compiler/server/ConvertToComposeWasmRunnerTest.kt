package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class ConvertToComposeWasmRunnerTest : BaseExecutorTest() {
    @Test
    fun `base execute test`() {
        runComposeWasm(
            code = "fun main() {\n println(\"Hello, Compose!\")\n}",
            contains = "Hello, Compose!"
        )
    }
}
