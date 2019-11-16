package com.compiler.server

import com.compiler.server.compiler.components.KotlinProjectExecutor
import com.compiler.server.generator.TestProjectRunner
import com.compiler.server.generator.generateSingleProject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest


@SpringBootTest
class CommandLineArgumentsTest {
  @Autowired
  private lateinit var kotlinProjectExecutor: KotlinProjectExecutor

  @Autowired
  private lateinit var testRunner: TestProjectRunner

  @Test
  fun `command line arguments jvm test`() {
    testRunner.run(
      code = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[1])\n}",
      args = "0 1",
      contains = "0\n1\n"
    )
  }

  @Test
  fun `command line string arguments jvm test`() {
    testRunner.run(
      code = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[1])\n}",
      args = "alex1 alex2",
      contains = "alex1\nalex2\n"
    )
  }

  @Test
  fun `command line arguments js test`() {
    testRunner.runJs(
      code = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[1])\n}",
      args = "0 1",
      contains = "main(['0', '1']);"
    )
  }

  @Test
  fun `command line string arguments js test`() {
    testRunner.runJs(
      code = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[1])\n}",
      args = "alex1 alex2",
      contains = "main(['alex1', 'alex2']);"
    )
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