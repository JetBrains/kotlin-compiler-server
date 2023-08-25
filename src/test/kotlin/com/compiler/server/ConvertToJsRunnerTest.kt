package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class ConvertToJsIrRunnerTest : BaseExecutorTest() {
  @Test
  fun `base execute test`() {
    runJsIr(
      code = "fun main() {\n println(\"Hello, world!!!\")\n}",
      contains = "println('Hello, world!!!');"
    )
  }

  @Test
  fun `base execute test multi`() {
    runJsIr(
      code = listOf(
        "import cat.Cat\n\nfun main(args: Array<String>) {\nval cat = Cat(\"Kitty\")\nprintln(cat.name)\n}",
        "package cat\n    class Cat(val name: String)"
      ),
      contains = "var cat = new Cat('Kitty');"
    )
  }
}