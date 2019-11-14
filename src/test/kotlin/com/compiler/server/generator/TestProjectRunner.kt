package com.compiler.server.generator

import com.compiler.server.compiler.components.KotlinProjectExecutor
import com.compiler.server.compiler.model.TranslationJSResult
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TestProjectRunner {
  @Autowired
  private lateinit var kotlinProjectExecutor: KotlinProjectExecutor

  fun run(code: String, contains: String) {
    val project = generateSingleProject(text = code)
    val result = kotlinProjectExecutor.run(project)
    Assertions.assertNotNull(result)
    Assertions.assertTrue(result.text.contains(contains))
  }

  fun runJs(code: String, contains: String) {
    val project = generateSingleProject(text = code)
    val result = kotlinProjectExecutor.convertToJs(project) as? TranslationJSResult
    Assertions.assertNotNull(result)
    Assertions.assertTrue(result!!.jsCode!!.contains(contains))
  }
}