package com.compiler.server

import com.compiler.server.generator.TestProjectRunner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ExceptionInProgramTest {
  @Autowired
  private lateinit var testRunner: TestProjectRunner

  @Test
  fun `command line index of bound jvm`() = testRunner.runWithException(
    code = "fun main(args: Array<String>) {\n    println(args[0])\n    println(args[2])\n}",
    contains = "java.lang.ArrayIndexOutOfBoundsException"
  )

  @Test
  fun `security read file`() = testRunner.runWithException(
    code = "import java.io.*\n\nfun main() {\n    val f = File(\"executor.policy\")\n    print(f.toURL())\n}",
    contains = "java.security.AccessControlException"
  )

}