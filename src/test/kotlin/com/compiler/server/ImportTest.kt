package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ProjectSeveriry
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ImportTest : BaseExecutorTest() {
  @Test
  fun `base import ok`() {
    val highlights = highlight("import kotlin.random.Random\n" +
      "\n" +
      "fun main() {\n" +
      "    val randVal = Random(3)\n" +
      "    println(randVal.nextInt())\n" +
      "}")
    Assertions.assertTrue(highlights.values.flatten().isEmpty())
  }

  @Test
  fun `highlight  Unresolved reference`() {
    val highlights = highlight("" +
      "fun main() {\n" +
      "    val randVal = Random(3)\n" +
      "    println(randVal.nextInt())\n" +
      "}")
    errorContains(highlights, "Unresolved reference: Random")
  }

  @Test
  fun `math import`() {
    complete(
      code = "import kotlin.math.PI\n" +
        "\n" +
        "fun main() {\n" +
        "    val pi = PI\n" +
        "    println(pi)\n" +
        "    val alex = 1\n" +
        "    val alex1 = 1 + a\n" +
        "}",
      line = 6,
      character = 21,
      completions = listOf("alex")
    )
  }

  @Test
  fun `unresolved import`() {
    val highlights = highlight("" +
      "import kotlin.random.Hello\n" +
      "\n" +
      "fun main() {\n" +
      "}")
    errorContains(highlights, "Unresolved reference: Hello")
  }

  @Language("kotlin")
  @Test
  fun `unresolved gggimport`() {
    val highlights = highlight("""
            import kotlin.collections.MutableMap
            fun foo(): ArrayList<String> = ArrayList<String>()

            fun main() {
            }
        """.trimIndent())
    errorContains(highlights, "Unresolved reference: Hello")
  }

  @Test
  fun `test fun`() {
    val res = PackagePartClassUtils.getPackagePartFqName(FqName("kotlin.io"), "Exception")
    println(res)
  }

  private fun errorContains(highlights: Map<String, List<ErrorDescriptor>>, message: String) {
    Assertions.assertTrue(highlights.values.flatten().map { it.message }.any { it.contains(message) })
    Assertions.assertTrue(highlights.values.flatten().map { it.severity }.any { it == ProjectSeveriry.ERROR })
  }
}