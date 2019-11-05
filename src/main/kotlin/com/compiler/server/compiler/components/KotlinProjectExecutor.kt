package com.compiler.server.compiler.components

import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.model.Completion
import com.compiler.server.compiler.model.ErrorDescriptor
import com.compiler.server.compiler.model.JavaExecutionResult
import com.compiler.server.compiler.model.Project
import org.apache.commons.logging.LogFactory
import org.springframework.stereotype.Component

@Component
class KotlinProjectExecutor(
  private val kotlinCompiler: KotlinCompiler,
  private val completionProvider: CompletionProvider,
  private val errorAnalyzer: ErrorAnalyzer,
  private val kotlinEnvironment: KotlinEnvironment
) {

  private val log = LogFactory.getLog(KotlinProjectExecutor::class.java)

  fun run(project: Project): JavaExecutionResult {
    val files = getFilesFrom(project)
    return kotlinCompiler.run(files)
  }

  fun complete(project: Project, line: Int, character: Int): List<Completion> {
    val file = getFilesFrom(project).first()
    return try {
      completionProvider.complete(file, line, character)
    }
    catch (e: Exception) {
      log.warn("Exception in getting completions", e)
      emptyList()
    }
  }

  fun highlight(project: Project): Map<String, List<ErrorDescriptor>> {
    val files = getFilesFrom(project).map { it.kotlinFile }
    return try {
      errorAnalyzer.errorsFrom(files)
    }
    catch (e: Exception) {
      log.warn("Exception in getting highlight", e)
      emptyMap()
    }
  }

  private fun getFilesFrom(project: Project) = project.files.map {
    KotlinFile.from(kotlinEnvironment.coreEnvironment.project, it.name, it.text)
  }
}