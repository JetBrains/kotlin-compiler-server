package com.compiler.server.generator

import com.compiler.server.compiler.components.KotlinProjectExecutor
import com.compiler.server.compiler.model.Project
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TestProjectRunner {
  @Autowired
  private lateinit var kotlinProjectExecutor: KotlinProjectExecutor

  fun run(code: String, contains: String) {
    val project = generateSingleProject(text = code)
    runAndTest(project, contains)
  }

  fun multiRun(code: List<String>, contains: String) {
    val project = generateMultiProject(*code.toTypedArray())
    runAndTest(project, contains)
  }

  fun runJs(code: String, contains: String) {
    val project = generateSingleProject(text = code)
    convertAndTest(project, contains)
  }

  fun multiRunJs(code: List<String>, contains: String) {
    val project = generateMultiProject(*code.toTypedArray())
    convertAndTest(project, contains)
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