package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class CompilerVersionTest: BaseExecutorTest() {


    @Test
    fun `current version test`() {
        run(
                code = "fun main(args: Array<String>) {\n" +
                        "    println(KotlinVersion.CURRENT)\n" +
                        "}",
                contains = "1.2.71"
        )
    }

}