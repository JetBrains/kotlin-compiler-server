package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.errorContains
import org.junit.jupiter.api.Test

class CompilerCrashTest : BaseExecutorTest() {

    @Test
    fun `test duplicate type parameter in when expression crash`() {
        val code = """
            fun <T, T> f() {
                when (T) {
                }
            }
            
            
            fun main() {
                println("Hello")
            }
        """.trimIndent()

        val highlights = highlight(code)
        errorContains(highlights, "org.jetbrains.kotlin.util.FileAnalysisException")
        errorContains(highlights, "Not supported: class org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl")
    }

    @Test
    fun `test duplicate type parameter in when expression crash js`() {
        val code = """
            fun <T, T> f() {
                when (T) {
                }
            }
            
            fun main() {
                println("Hello")
            }
        """.trimIndent()

        val highlights = highlightJS(code)
        errorContains(highlights, "org.jetbrains.kotlin.util.FileAnalysisException")
        errorContains(highlights, "Not supported: class org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl")
    }

    @Test
    fun `test duplicate type parameter in when expression crash wasm`() {
        val code = """
            fun <T, T> f() {
                when (T) {
                }
            }
            
            fun main() {
                println("Hello")
            }
        """.trimIndent()

        val highlights = highlightWasm(code)
        errorContains(highlights, "org.jetbrains.kotlin.util.FileAnalysisException")
        errorContains(highlights, "Not supported: class org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl")
    }
}
