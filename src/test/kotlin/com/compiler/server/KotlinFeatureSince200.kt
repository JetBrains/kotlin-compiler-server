package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.assertNoErrors
import org.junit.jupiter.api.Test

class KotlinFeatureSince200 : BaseExecutorTest() {
    @Test
    fun `smart cast to for variables`() {
        run(
            // language=kotlin
            code = """
                class Cat {
                    fun purr() {
                        println("Purr purr")
                    }
                }

                fun petAnimal(animal: Any) {
                    val isCat = animal is Cat
                    if (isCat) {
                        // In K2, the compiler can access
                        // information about isCat, so it knows that
                        // isCat was smart cast to type Cat.
                        // Therefore, the purr() function is successfully called.
                        // In Kotlin 1.9.20, the compiler doesn't know
                        // about the smart cast so calling the purr()
                        // function triggers an error.
                        animal.purr()
                   }
                }

                fun main() {
                    val kitty = Cat()
                    petAnimal(kitty)
                    // Purr purr
                }
            """.trimIndent(),
            contains = "Purr purr"
        )
    }

    @Test
    fun `stable replacement of the enum class values function`() {
        val result = run(
            // language=kotlin
            code = """
                fun main() {
                    var stringInput: String? = null
                    // stringInput is smart cast to String type
                    stringInput = ""
                    try {
                        // The compiler knows that stringInput isn't null
                        println(stringInput.length)
                        // 0
                
                        // The compiler rejects previous smart cast information for
                        // stringInput. Now stringInput has String? type.
                        stringInput = null
                
                        // Trigger an exception
                        if (2 > 1) throw Exception()
                        stringInput = ""
                    } catch (exception: Exception) {
                        // In Kotlin %kotlinEapVersion%, the compiler knows stringInput
                        // can be null so stringInput stays nullable.
                        println(stringInput?.length)
                        // null
                
                        // In Kotlin 1.9.20, the compiler says that a safe call isn't
                        // needed, but this is incorrect.
                    }
                }
            """.trimIndent(),
            contains = "0\nnull"
        ).assertNoErrors()
    }
}

