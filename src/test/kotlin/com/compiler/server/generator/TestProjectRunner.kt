package com.compiler.server.generator

import com.compiler.server.model.Project
import com.compiler.server.service.KotlinProjectExecutor
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TestProjectRunner {
  @Autowired
  private lateinit var kotlinProjectExecutor: KotlinProjectExecutor

  fun run(code: String, contains: String, args: String = "") {
    val project = generateSingleProject(text = code, args = args)
    runAndTest(project, contains)
  }

  fun multiRun(code: List<String>, contains: String) {
    val project = generateMultiProject(*code.toTypedArray())
    runAndTest(project, contains)
  }

  fun runJs(code: String, contains: String, args: String = "") {
    val project = generateSingleProject(text = code, args = args, isJs = true)
    convertAndTest(project, contains)
  }

  fun multiRunJs(code: List<String>, contains: String) {
    val project = generateMultiProject(*code.toTypedArray(), isJs = true)
    convertAndTest(project, contains)
  }

  fun complete(code: String, line: Int, character: Int, completions: List<String>, isJs: Boolean = false) {
    val project = generateSingleProject(text = code, isJs = isJs)
    val result = kotlinProjectExecutor.complete(project, line, character)
      .map { it.displayText }
    Assertions.assertTrue(result.isNotEmpty())
    completions.forEach { suggest ->
      Assertions.assertTrue(result.contains(suggest))
    }
  }

  private fun runAndTest(project: Project, contains: String) {
    val result = kotlinProjectExecutor.run(project)
    Assertions.assertNotNull(result)
    Assertions.assertTrue(result.text.contains(contains))
  }

  private fun convertAndTest(project: Project, contains: String) {
    val result = kotlinProjectExecutor.convertToJs(project)
    Assertions.assertNotNull(result)
    Assertions.assertTrue(result.jsCode!!.contains(contains))
  }
}