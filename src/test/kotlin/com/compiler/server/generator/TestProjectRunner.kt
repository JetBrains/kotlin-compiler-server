package com.compiler.server.generator

import com.compiler.server.model.*
import com.compiler.server.service.KotlinProjectExecutor
import common.model.Completion
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TestProjectRunner {
  @Autowired
  private lateinit var kotlinProjectExecutor: KotlinProjectExecutor

  fun run(code: String, contains: String, args: String = ""): ExecutionResult {
    val project = generateSingleProject(text = code, args = args)
    return runAndTest(project, contains)
  }

  fun multiRun(code: List<String>, contains: String) {
    val project = generateMultiProject(*code.toTypedArray())
    runAndTest(project, contains)
  }

  fun runJs(code: String, contains: String, args: String = "") {
    val project = generateSingleProject(text = code, args = args, projectType = ProjectType.JS)
    convertAndTest(project, contains)
  }

  fun multiRunJs(code: List<String>, contains: String) {
    val project = generateMultiProject(*code.toTypedArray(), projectType = ProjectType.JS)
    convertAndTest(project, contains)
  }

  fun translateToJs(code: String): TranslationJSResult {
    val project = generateSingleProject(text = code, projectType = ProjectType.JS)
    return kotlinProjectExecutor.convertToJs(project)
  }

  fun runWithException(code: String, contains: String): ExecutionResult {
    val project = generateSingleProject(text = code)
    val result = kotlinProjectExecutor.run(project)
    Assertions.assertNotNull(result.exception, "Test result should no be a null")
    Assertions.assertTrue(
      result.exception?.fullName?.contains(contains) == true,
      "Actual: ${result.exception?.message}, Expected: $contains"
    )
    return result
  }

  fun test(vararg test: String): List<TestDescription> {
    val project = generateMultiProject(*test, projectType = ProjectType.JUNIT)
    val result = kotlinProjectExecutor.test(project) as? JunitExecutionResult
    Assertions.assertNotNull(result?.testResults, "Test result should no be a null")
    return result?.testResults?.values?.flatten() ?: emptyList()
  }

  fun testRaw(vararg test: String): JunitExecutionResult? = executeTest(*test)

  fun complete(
    code: String,
    line: Int,
    character: Int,
    completions: List<String>,
    isJs: Boolean = false
  ) {
    val type = if (isJs) ProjectType.JS else ProjectType.JAVA
    val project = generateSingleProject(text = code, projectType = type)
    val result = kotlinProjectExecutor.complete(project, line, character)
      .map { it.displayText }
    Assertions.assertTrue(result.isNotEmpty())
    completions.forEach { suggest ->
      Assertions.assertTrue(result.contains(suggest))
    }
  }

  fun getCompletions(
    code: String,
    line: Int,
    character: Int,
    isJs: Boolean = false
  ): List<Completion> {
    val type = if (isJs) ProjectType.JS else ProjectType.JAVA
    val project = generateSingleProject(text = code, projectType = type)
    return kotlinProjectExecutor.complete(project, line, character)
  }

  fun highlight(code: String): Map<String, List<ErrorDescriptor>> {
    val project = generateSingleProject(text = code)
    return kotlinProjectExecutor.highlight(project)
  }

  fun highlightJS(code: String): Map<String, List<ErrorDescriptor>> {
    val project = generateSingleProject(text = code, projectType = ProjectType.JS)
    return kotlinProjectExecutor.highlight(project)
  }

  fun getVersion() = kotlinProjectExecutor.getVersion().version

  private fun executeTest(vararg test: String): JunitExecutionResult? {
    val project = generateMultiProject(*test, projectType = ProjectType.JUNIT)
    return kotlinProjectExecutor.test(project) as? JunitExecutionResult
  }

  private fun runAndTest(project: Project, contains: String): ExecutionResult {
    val result = kotlinProjectExecutor.run(project)
    Assertions.assertNotNull(result, "Test result should no be a null")
    Assertions.assertTrue(result.text.contains(contains) == true, "Actual: ${result.text}. \n Expected: $contains")
    return result
  }

  private fun convertAndTest(project: Project, contains: String) {
    val result = kotlinProjectExecutor.convertToJs(project)
    Assertions.assertNotNull(result, "Test result should no be a null")
    Assertions.assertTrue(result.jsCode!!.contains(contains), "Actual: ${result.jsCode}. \n Expected: $contains")
  }
}