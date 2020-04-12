package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.TestCompiler

class JvmRunnerTest : BaseExecutorTest() {

  @TestCompiler
  fun `base execute test JVM`() {
    run(
      code = "fun main() {\n println(\"Hello, world!!!\")\n}",
      contains = "Hello, world!!!"
    )
  }

  @TestCompiler
  fun `no main class jvm test`() {
    run(
      code = "fun main1() {\n    println(\"sdf\")\n}",
      contains = "No main method found in project"
    )
  }

  @TestCompiler
  fun `base execute test JVM multi`() {
    run(
      code = listOf(
        "import cat.Cat\n\nfun main(args: Array<String>) {\nval cat = Cat(\"Kitty\")\nprintln(cat.name)\n}",
        "package cat\n    class Cat(val name: String)"
      ),
      contains = "Kitty"
    )
  }

  @TestCompiler
  fun `correct kotlin version test jvm`() {
    run(
      code = "fun main() {\n    println(KotlinVersion?.CURRENT)\n}",
      contains = version().substringBefore("-")
    )
  }

}
