package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class KotlinFeatureSince20 : BaseExecutorTest() {

    @Test
    fun `Support UUIDs`() {
        run(
            // language=kotlin
            code = """
                import kotlin.uuid.*

                fun main(args: Array<String>) {
                    val uuid = Uuid.parse("550E8400-e29b-41d4-A716-446655440000")
                    println(uuid)
                }
            """.trimIndent(),
            contains = "550e8400-e29b-41d4-a716-446655440000"
        )
    }
}

