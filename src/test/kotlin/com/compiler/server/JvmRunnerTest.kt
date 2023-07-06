package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import component.KotlinEnvironment
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JvmRunnerTest : BaseExecutorTest() {

  @Test
  fun  `jvm test setting java 11`() {
    val kv  = KotlinEnvironment(emptyList(), emptyList(), listOf("-jvm-target=11"))
    kv.environment {
      Assertions.assertEquals(JvmTarget.JVM_11, it.configuration.get(JVMConfigurationKeys.JVM_TARGET), "JVM target should be java 11")
    }
  }

  @Test
  fun  `jvm test default`() {
    val kv  = KotlinEnvironment(emptyList(), emptyList(), emptyList())
    kv.environment {
      Assertions.assertEquals(null, it.configuration.get(JVMConfigurationKeys.JVM_TARGET), "JVM target is not set")
    }
  }

  @Test
  fun `base execute test JVM`() {
    run(
      code = "fun main() {\n println(\"Hello, world!!!\")\n}",
      contains = "Hello, world!!!"
    )
  }

  @Test
  fun `no main class jvm test`() {
    run(
      code = "fun main1() {\n    println(\"sdf\")\n}",
      contains = "No main method found in project"
    )
  }

  @Test
  fun `base execute test JVM multi`() {
    run(
      code = listOf(
        "import cat.Cat\n\nfun main(args: Array<String>) {\nval cat = Cat(\"Kitty\")\nprintln(cat.name)\n}",
        "package cat\n    class Cat(val name: String)"
      ),
      contains = "Kitty"
    )
  }

  @Test
  fun `correct kotlin version test jvm`() {
    run(
      code = "fun main() {\n    println(KotlinVersion?.CURRENT)\n}",
      contains = version().substringBefore("-")
    )
  }

}
