package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class KotlinFeatureSince220 : BaseExecutorTest() {

    @Test
    fun `Example of context parameters`() {
        run(
            code = """
            class Logger {
              fun info(message: String) = println(message)
            }
    
            interface LoggingContext {
              val log: Logger
            }
    
            context(loggingContext : LoggingContext)
            fun executeTask() {
              loggingContext.log.info("Complete")
            }
    
            fun main() {
              val loggingContext = object: LoggingContext {
                override val log = Logger()
              }
                
              with(loggingContext) {
                executeTask()
              }
            }
            """.trimIndent(),
            contains = "Complete"
        )
    }

    @Test
    fun `Example of nested type aliases`() {
        run(
            code = """
            class MyName(val name: String) {
    
                typealias Context = String
                
                fun printMyName(nameToPrint: Context = name) {
                    println(nameToPrint)
                }
                
            }

            fun main() {
                MyName("I'm nested type alias").printMyName()
            }
            """.trimIndent(),
            contains = "I'm nested type alias"
        )
    }
}
