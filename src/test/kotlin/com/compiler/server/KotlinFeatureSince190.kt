package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class KotlinFeatureSince190 : BaseExecutorTest() {

    @Test
    fun `stable replacement of the enum class values function`() {
        run(
            // language=kotlin
            code = """
                enum class Color(val colorName: String, val rgb: String) {
                    RED("Red", "#FF0000"),
                    ORANGE("Orange", "#FF7F00"),
                    YELLOW("Yellow", "#FFFF00")
                }
                
                fun findByRgb(rgb: String): Color? = Color.entries.find { it.rgb == rgb }
                
                fun main() {
                    val color = findByRgb("#FF0000")
                    println("Color is ${'$'}{color?.colorName}")
                }
            """.trimIndent(),
            contains = "Color is Red"
        )
    }

    @Test
    fun `stable data objects for symmetry with data classes`() {
        run(
            // language=kotlin
            code = """
                sealed interface ReadResult
                data class Number(val number: Int) : ReadResult
                data class Text(val text: String) : ReadResult
                data object EndOfFile : ReadResult
                
                fun main() {
                    println(Number(7)) // Number(number=7)
                    println(EndOfFile) // EndOfFile
                }
            """.trimIndent(),
            contains = "Number(number=7)\nEndOfFile"
        )
    }

    @Test
    fun `calculate the time difference between multiple TimeMarks`() {
        run(
            //language=kotlin
            code = """
                @JvmInline
                value class Person(private val fullName: String) {
                    // Allowed since Kotlin 1.4.30:
                    init {
                        check(fullName.isNotBlank()) {
                            "Full name shouldn't be empty"
                        }
                    }
                    // Allowed by default since Kotlin 1.9.0:
                    constructor(name: String, lastName: String) : this("${'$'}name ${'$'}lastName") {
                        check(lastName.isNotBlank()) {
                            "Last name shouldn't be empty"
                        }
                    }
                }
                
                fun main() {
                    println(Person("John", "Smith"))
                }
            """.trimIndent(),
            contains = "Person(fullName=John Smith)"
        )
    }
}

