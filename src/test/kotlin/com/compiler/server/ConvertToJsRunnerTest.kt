package com.compiler.server

import com.compiler.server.generator.TestProjectRunner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ConvertToJsRunnerTest {
  @Autowired
  private lateinit var testRunner: TestProjectRunner

  @Test
  fun `base execute test`() = testRunner.runJs(
    code = "fun main() {\n println(\"Hello, world!!!\")\n}",
    contains = "println('Hello, world!!!');"
  )

  @Test
  fun `base execute test multi`() = testRunner.multiRunJs(
    code = listOf(
      "import cat.Cat\n\nfun main(args: Array<String>) {\nval cat = Cat(\"Kitty\")\nprintln(cat.name)\n}",
      "package cat\n    class Cat(val name: String)"
    ),
    contains = "var cat = new Cat('Kitty');"
  )
}