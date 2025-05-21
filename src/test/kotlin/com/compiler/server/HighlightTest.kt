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
    Assertions.assertTrue(highlights.isEmpty()) { highlights.toString() }
  }

  @Test
  fun `base highlight js ok`() {
    val highlights = highlightJS("\nfun main() {\n    println(\"Hello, world!!!\")\n}")
    Assertions.assertTrue(highlights.isEmpty()) { highlights.toString() }
  }

  @Test
  fun `base highlight wasm ok`() {
    val highlights = highlightWasm("\nfun main() {\n    println(\"Hello, world!!!\")\n}")
    Assertions.assertTrue(highlights.isEmpty())
  }

  @Test
  fun `base highlight unused variable`() {
    val highlights = highlight("fun main() {\n\tval a = \"d\"\n}")
    warningContains(highlights, "Variable is unused")
  }

  @Test
  fun `base highlight unused variable js`() {
    val highlights = highlightJS("fun main() {\n\tval a = \"d\"\n}")
    warningContains(highlights, "Variable is unused")
  }

  @Test
  fun `base highlight unused variable wasm`() {
    val highlights = highlightWasm("fun main() {\n\tval a = \"d\"\n}")
    warningContains(highlights, "Variable is unused")
  }

  @Test
  fun `base highlight false condition`() {
    val highlights = highlight("fun main() {\n    val a: String = \"\"\n    if (a == null) print(\"b\")\n}")
    warningContains(highlights, "Condition is always 'false'.")
  }

  @Test
  fun `base highlight false condition js`() {
    val highlights = highlightJS("fun main() {\n    val a: String = \"\"\n    if (a == null) print(\"b\")\n}")
    warningContains(highlights, "Condition is always 'false'.")
  }

  @Test
  fun `base highlight false condition wasm`() {
    val highlights = highlightWasm("fun main() {\n    val a: String = \"\"\n    if (a == null) print(\"b\")\n}")
    warningContains(highlights, "Condition is always 'false'.")
  }

  @Test
  fun `highlight  Unresolved reference`() {
    val highlights = highlight("fun main() {\n   dfsdf\n}")
    errorContains(highlights, "Unresolved reference 'dfsdf'.")
  }

  @Test
  fun `highlight js Unresolved reference`() {
    val highlights = highlightJS("fun main() {\n   dfsdf\n}")
    errorContains(highlights, "Unresolved reference 'dfsdf'.")
  }

  @Test
  fun `highlight wasm Unresolved reference`() {
    val highlights = highlightWasm("fun main() {\n   dfsdf\n}")
    errorContains(highlights, "Unresolved reference 'dfsdf'.")
  }

  @Test
  fun `highlight Type inference failed`() {
    val highlights = highlight("fun main() {\n   \"sdf\".to\n}")
    Assertions.assertEquals(highlights.size, 2)
    errorContains(highlights, "Cannot infer type for type parameter 'B'. Specify it explicitly.")
    errorContains(highlights, "Function invocation 'to(...)' expected")
  }

  @Test
  fun `highlight js Type inference failed`() {
    val highlights = highlightJS("fun main() {\n   \"sdf\".to\n}")
    Assertions.assertEquals(highlights.size, 2)
    errorContains(highlights, "Cannot infer type for type parameter 'B'. Specify it explicitly.")
    errorContains(highlights, "Function invocation 'to(...)' expected")
  }

  @Test
  fun `highlight wasm Type inference failed`() {
    val highlights = highlightWasm("fun main() {\n   \"sdf\".to\n}")
    Assertions.assertEquals(highlights.size, 2)
    errorContains(highlights, "Cannot infer type for type parameter 'B'. Specify it explicitly.")
    errorContains(highlights, "Function invocation 'to(...)' expected")
  }
}
