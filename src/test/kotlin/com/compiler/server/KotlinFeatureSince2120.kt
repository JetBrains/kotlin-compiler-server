package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class KotlinFeatureSince2120 : BaseExecutorTest() {

    @Test
    fun `Support atomics`() {
        run(
            // language=kotlin
            code = """
                import kotlin.concurrent.atomics.*

                fun main(args: Array<String>) {
                    println(AtomicInt(42))
                }
            """.trimIndent(),
            contains = "42"
        )
    }
}
