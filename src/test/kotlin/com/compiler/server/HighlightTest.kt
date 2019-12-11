package com.compiler.server

import com.compiler.server.generator.TestProjectRunner
import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ProjectSeveriry
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class HighlightTest {
  @Autowired
  private lateinit var testRunner: TestProjectRunner

  @Test
  fun `base highlight ok`(){
    val highlights = testRunner.highlight("\nfun main() {\n    println(\"Hello, world!!!\")\n}")
    Assertions.assertTrue(highlights.values.flatten().isEmpty())
  }

  @Test
  fun `base highlight js ok`(){
    val highlights = testRunner.highlightJS("\nfun main() {\n    println(\"Hello, world!!!\")\n}")
    Assertions.assertTrue(highlights.values.flatten().isEmpty())
  }

  @Test
  fun `base highlight unused variable`(){
    val highlights = testRunner.highlight("fun main() {\n\tval a = \"d\"\n}")
    warningContains(highlights, "Variable 'a' is never used")
  }

  @Test
  fun `base highlight unused variable js`(){
    val highlights = testRunner.highlightJS("fun main() {\n\tval a = \"d\"\n}")
    warningContains(highlights, "Variable 'a' is never used")
  }

  @Test
  fun `base highlight false condition`(){
    val highlights = testRunner.highlight("fun main() {\n    val a: String = \"\"\n    if (a == null) print(\"b\")\n}")
    warningContains(highlights, "Condition 'a == null' is always 'false'")
  }

  @Test
  fun `base highlight false condition js`(){
    val highlights = testRunner.highlightJS("fun main() {\n    val a: String = \"\"\n    if (a == null) print(\"b\")\n}")
    warningContains(highlights, "Condition 'a == null' is always 'false'")
  }

  @Test
  fun `highlight  Unresolved reference`(){
    val highlights = testRunner.highlight("fun main() {\n   dfsdf\n}")
    errorContains(highlights, "Unresolved reference: dfsdf")
  }

  @Test
  fun `highlight js Unresolved reference`(){
    val highlights = testRunner.highlightJS("fun main() {\n   dfsdf\n}")
    errorContains(highlights, "Unresolved reference: dfsdf")
  }

  @Test
  fun `highlight Type inference failed`(){
    val highlights = testRunner.highlight("fun main() {\n   \"sdf\".to\n}")
    errorContains(highlights, "Type inference failed: Not enough information to infer parameter B in infix fun <A, B> A.to(that: B): Pair<A, B>")
    errorContains(highlights, "No value passed for parameter 'that'")
    errorContains(highlights, "Function invocation 'to(...)' expected")
  }

  @Test
  fun `highlight js Type inference failed`(){
    val highlights = testRunner.highlightJS("fun main() {\n   \"sdf\".to\n}")
    errorContains(highlights, "Type inference failed: Not enough information to infer parameter B in infix fun <A, B> A.to(that: B): Pair<A, B>")
    errorContains(highlights, "No value passed for parameter 'that'")
    errorContains(highlights, "Function invocation 'to(...)' expected")
  }

  private fun errorContains(highlights: Map<String, List<ErrorDescriptor>>, message: String) {
    Assertions.assertTrue(highlights.values.flatten().map { it.message }.any { it.contains(message) })
    Assertions.assertTrue(highlights.values.flatten().map { it.severity }.any { it == ProjectSeveriry.ERROR })
  }

  private fun warningContains(highlights: Map<String, List<ErrorDescriptor>>, message: String){
    Assertions.assertTrue(highlights.values.flatten().map { it.message }.any { it.contains(message) })
    Assertions.assertTrue(highlights.values.flatten().map { it.className }.any { it == "WARNING" })
    Assertions.assertTrue(highlights.values.flatten().map { it.severity }.any { it == ProjectSeveriry.WARNING })
  }
}