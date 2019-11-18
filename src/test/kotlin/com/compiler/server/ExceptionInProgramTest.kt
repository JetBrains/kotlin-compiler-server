package com.compiler.server

import com.compiler.server.generator.TestProjectRunner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ExceptionInProgramTest{
  @Autowired
  private lateinit var testRunner: TestProjectRunner

  @Test
  fun `no main class jvm test`() = testRunner.runWithException(
    code = "fun main1() {\n    println(\"sdf\")\n}",
    contains = "Error: Could not find or load main class"
  )

}