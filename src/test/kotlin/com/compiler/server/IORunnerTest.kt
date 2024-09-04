package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class IORunnerTest : BaseExecutorTest() {

    @Test
    fun `kotlinx io basic test`() {
        run(
            code = """
            import kotlinx.io.*
            
            fun main() {
                val source = "Hello world".byteInputStream().asSource()
                val buffered = source.buffered()
                println(buffered.readString())
                
            }
          """.trimIndent(),
            contains = "<outStream>Hello world"
        )
    }

    @Test
    fun `kotlinx io bytestring test`() {
        run (
            code = """
                import kotlinx.io.*
                import kotlinx.io.bytestring.*
                
                fun main() {
                    val str = ByteString("Hello world".encodeToByteArray())
                    val buffer = Buffer().also { it.write(str) }
                    println(buffer.readString())
                }
            """.trimIndent(),
            contains = "<outStream>Hello world"
        )
    }
}