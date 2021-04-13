package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.assertNoErrors
import org.junit.jupiter.api.Test

class KotlinFeatureSince150 : BaseExecutorTest() {
    @Test
    fun `KT-45991 on JVM IR BE`() {
        run(
                code = """
                fun ex1_(res: Result<Int>) {
                    res.fold(
                        onSuccess = { println("Ex " + it) },
                        onFailure = {},
                    )
                }
                
                fun ex1(res: Result<Int>) {
                    res.fold(
                        onSuccess = { ex1_(res) },
                        onFailure = { ex1_(Result.failure(it)) }
                    )
                }
                
                val ex2_: (Result<Int>) -> Unit = { res ->
                    res.fold(
                        onSuccess = { println("Ex " + it) },
                        onFailure = {},
                    )
                }
                
                val ex2: (Result<Int>) -> Unit = { res ->
                    res.fold(
                        onSuccess = { ex2_(Result.success(it)) },
                        onFailure = { ex2_(Result.failure(it)) }
                    )
                }
                
                
                val ex3_: (Result<Int>) -> Unit = { res ->
                    res.fold(
                        onSuccess = { println("Ex " + it) },
                        onFailure = {},
                    )
                }
                
                val ex3: (Result<Int>) -> Unit = { res ->
                    res.fold(
                        onSuccess = { ex3_(res) },
                        onFailure = { ex3_(Result.failure(it)) }
                    )
                }
                
                fun main() {
                    ex1(Result.success(1)) // works
                    ex2(Result.success(2)) // works
                    ex3(Result.success(3)) // doesn't work
                }
                """.trimIndent(),
                contains = "Ex 1\n" +
                        "Ex 2\n" +
                        "Ex 3"
        ).assertNoErrors()
    }

}