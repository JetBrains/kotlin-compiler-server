package com.compiler.server

import com.compiler.server.compiler.components.KotlinProjectExecutor
import com.compiler.server.generator.generateSingleProject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest


@SpringBootTest
class CommandLineArgumentsTest {
  @Autowired
  private lateinit var kotlinProjectExecutor: KotlinProjectExecutor

  @Test
  fun `command line arguments jvm test`() {
    val project = generateSingleProject(
      text = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[1])\n}",
      args = "0 1"
    )
    val result = kotlinProjectExecutor.run(project)
    Assertions.assertNotNull(result)
    Assertions.assertTrue(result.text.contains("0\n1\n"))
  }

  @Test
  fun `command line string arguments jvm test`() {
    val project = generateSingleProject(
      text = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[1])\n}",
      args = "alex1 alex2"
    )
    val result = kotlinProjectExecutor.run(project)
    Assertions.assertNotNull(result)
    Assertions.assertTrue(result.text.contains("alex1\nalex2\n"))
  }
  @Test
  fun `command line arguments js test`() {
    val project = generateSingleProject(
      text = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[1])\n}",
      args = "0 1"
    )
    val result = kotlinProjectExecutor.convertToJs(project)
    Assertions.assertNotNull(result)
    Assertions.assertTrue(result.jsCode!!.contains("main(['0', '1']);"))
  }

  @Test
  fun `command line string arguments js test`() {
    val project = generateSingleProject(
      text = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[1])\n}",
      args = "alex1 alex2"
    )
    val result = kotlinProjectExecutor.convertToJs(project)
    Assertions.assertNotNull(result)
    Assertions.assertTrue(result.jsCode!!.contains("main(['alex1', 'alex2']);"))
  }

  @Test
  fun `command line index of bound jvm`(){
    val project = generateSingleProject(
      text = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[2])\n}",
      args = "0 1"
    )
    val result = kotlinProjectExecutor.run(project)
    Assertions.assertNotNull(result.exception)
    Assertions.assertTrue(result.exception!!.message.contains("java.lang.ArrayIndexOutOfBoundsException"))
  }

}