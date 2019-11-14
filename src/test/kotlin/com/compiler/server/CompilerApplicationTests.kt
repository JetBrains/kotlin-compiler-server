package com.compiler.server

import com.compiler.server.compiler.components.KotlinProjectExecutor
import com.compiler.server.compiler.model.JavaExecutionResult
import com.compiler.server.compiler.model.TranslationJSResult
import com.compiler.server.generator.generateSingleProject
import com.compiler.server.generator.runManyTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CompilerApplicationTests {

  @Autowired
  private lateinit var kotlinProjectExecutor: KotlinProjectExecutor

  @Test
  fun `base execute test JVM`() {
    baseHelloWordRunnerTest()
  }

  @Test
  fun `base execute test JS`() {
    baseHelloWordTranslateTest()
  }

  @Test
  fun `a lot of hello word test JVM`() {
    runManyTest {
      baseHelloWordRunnerTest()
    }
  }

  @Test
  fun `a lot of hello word test JS`() {
    runManyTest {
      baseHelloWordTranslateTest()
    }
  }

  private fun baseHelloWordRunnerTest() {
    val project = generateSingleProject(
      text = """
            fun main() {
            println("Hello, world!!!")
            }  
          """.trimIndent()
    )
    val result = kotlinProjectExecutor.run(project)
    Assertions.assertNotNull(result)
    Assertions.assertTrue(result.text.contains("Hello, world!!!"))
  }

  private fun baseHelloWordTranslateTest() {
    val project = generateSingleProject(
      text = """
            fun main() {
            println("Hello, world!!!")
            }  
          """.trimIndent()
    )
    val result = kotlinProjectExecutor.convertToJs(project) as? TranslationJSResult
    Assertions.assertNotNull(result)
    Assertions.assertTrue(result!!.jsCode!!.contains("println('Hello, world!!!');"))
  }

}
