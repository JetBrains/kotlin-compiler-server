package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.ExecutorMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class JvmRunnerTest : BaseExecutorTest() {

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `base execute test JVM`(mode: ExecutorMode) {
    run(
      mode = mode,
      code = "fun main() {\n println(\"Hello, world!!!\")\n}",
      contains = "Hello, world!!!"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `no main class jvm test`(mode: ExecutorMode) {
    run(
      mode = mode,
      code = "fun main1() {\n    println(\"sdf\")\n}",
      contains = "No main method found in project"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `base execute test JVM multi`(mode: ExecutorMode) {
    run(
      mode = mode,
      code = listOf(
        "import cat.Cat\n\nfun main(args: Array<String>) {\nval cat = Cat(\"Kitty\")\nprintln(cat.name)\n}",
        "package cat\n    class Cat(val name: String)"
      ),
      contains = "Kitty"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `correct kotlin version test jvm`(mode: ExecutorMode) {
    run(
      mode = mode,
      code = "fun main() {\n    println(KotlinVersion?.CURRENT)\n}",
      contains = version().substringBefore("-")
    )
  }

}
