package com.compiler.server.generator

import com.compiler.server.model.*
import com.compiler.server.service.KotlinProjectExecutor
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TestProjectRunner {
  @Autowired
  private lateinit var kotlinProjectExecutor: KotlinProjectExecutor

  fun run(code: String, contains: String, args: String = ""): JavaExecutionResult {
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

  fun runWithException(code: String, contains: String) {
    val project = generateSingleProject(text = code)
    val result = kotlinProjectExecutor.run(project) as JavaExecutionResult
    Assertions.assertNotNull(result.exception, "Test result should no be a null")
    Assertions.assertTrue(result.exception?.message?.contains(contains) == true)
  }

  fun test(vararg test: String): List<TestDescription> {
    val project = generateMultiProject(*test, projectType = ProjectType.JUNIT)
    val result = kotlinProjectExecutor.test(project) as? JunitExecutionResult
    Assertions.assertNotNull(result?.testResults, "Test result should no be a null")
    return result?.testResults?.values?.flatten() ?: emptyList()
  }

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

  fun highlight(code: String): Map<String, List<ErrorDescriptor>> {
    val project = generateSingleProject(text = code)
    return kotlinProjectExecutor.highlight(project)
  }

  fun highlightJS(code: String): Map<String, List<ErrorDescriptor>> {
    val project = generateSingleProject(text = code, projectType = ProjectType.JS)
    return kotlinProjectExecutor.highlight(project)
  }

  fun getVersion() = kotlinProjectExecutor.getVersion().version

  private fun runAndTest(project: Project, contains: String): JavaExecutionResult {
    val result = kotlinProjectExecutor.run(project) as JavaExecutionResult?
    Assertions.assertNotNull(result, "Test result should no be a null")
    Assertions.assertTrue(result?.text?.contains(contains) == true, "Actual: ${result?.text}. Expected: $contains")
    return result!!
  }

  private fun convertAndTest(project: Project, contains: String) {
    val result = kotlinProjectExecutor.convertToJs(project)
    Assertions.assertNotNull(result, "Test result should no be a null")
    Assertions.assertTrue(result.jsCode!!.contains(contains), "Actual: ${result.jsCode}. Expected: $contains")
  }
}