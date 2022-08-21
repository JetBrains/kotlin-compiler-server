package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.errorContains
import com.compiler.server.base.warningContains
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class HighlightTest : BaseExecutorTest() {

  @Test
  fun `base highlight ok`() {
    val highlights = highlight("\nfun main() {\n    println(\"Hello, world!!!\")\n}")
    Assertions.assertTrue(highlights.values.flatten().isEmpty())
  }

  @Test
  fun `base highlight js ok`() {
    val highlights = highlightJS("\nfun main() {\n    println(\"Hello, world!!!\")\n}")
    Assertions.assertTrue(highlights.values.flatten().isEmpty())
  }

  @Test
  fun `base highlight unused variable`() {
    val highlights = highlight("fun main() {\n\tval a = \"d\"\n}")
    warningContains(highlights, "Variable 'a' is never used")
  }

  @Test
  fun `base highlight unused variable js`() {
    val highlights = highlightJS("fun main() {\n\tval a = \"d\"\n}")
    warningContains(highlights, "Variable 'a' is never used")
  }

  @Test
  fun `base highlight false condition`() {
    val highlights = highlight("fun main() {\n    val a: String = \"\"\n    if (a == null) print(\"b\")\n}")
    warningContains(highlights, "Condition 'a == null' is always 'false'")
  }

  @Test
  fun `base highlight false condition js`() {
    val highlights = highlightJS("fun main() {\n    val a: String = \"\"\n    if (a == null) print(\"b\")\n}")
    warningContains(highlights, "Condition 'a == null' is always 'false'")
  }

  @Test
  fun `highlight  Unresolved reference`() {
    val highlights = highlight("fun main() {\n   dfsdf\n}")
    errorContains(highlights, "Unresolved reference: dfsdf")
  }

  @Test
  fun `highlight js Unresolved reference`() {
    val highlights = highlightJS("fun main() {\n   dfsdf\n}")
    errorContains(highlights, "Unresolved reference: dfsdf")
  }

  @Test
  fun `highlight Type inference failed`() {
    val highlights = highlight("fun main() {\n   \"sdf\".to\n}")
    errorContains(
      highlights,
      "Not enough information to infer type variable B"
    )
    errorContains(highlights, "No value passed for parameter 'that'")
    errorContains(highlights, "Function invocation 'to(...)' expected")
  }

  @Test
  fun `highlight js Type inference failed`() {
    val highlights = highlightJS("fun main() {\n   \"sdf\".to\n}")
    errorContains(
      highlights,
      "Not enough information to infer type variable B"
    )
    errorContains(highlights, "No value passed for parameter 'that'")
    errorContains(highlights, "Function invocation 'to(...)' expected")
  }
}