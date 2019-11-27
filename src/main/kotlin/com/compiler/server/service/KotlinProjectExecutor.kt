package com.compiler.server.service

import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.components.*
import com.compiler.server.model.*
import org.apache.commons.logging.LogFactory
import org.springframework.stereotype.Component

@Component
class KotlinProjectExecutor(
  private val kotlinCompiler: KotlinCompiler,
  private val completionProvider: CompletionProvider,
  private val errorAnalyzer: ErrorAnalyzer,
  private val version: VersionInfo,
  private val kotlinToJSTranslator: KotlinToJSTranslator,
  private val kotlinEnvironment: KotlinEnvironment
) {

  private val log = LogFactory.getLog(KotlinProjectExecutor::class.java)

  fun run(project: Project): JavaExecutionResult {
    val files = getFilesFrom(project).map { it.kotlinFile }
    return kotlinCompiler.run(files, project.args)
  }

  fun test(project: Project): JunitExecutionResult {
    val files = getFilesFrom(project).map { it.kotlinFile }
    return kotlinCompiler.test(files)
  }

  fun convertToJs(project: Project): TranslationJSResult {
    val files = getFilesFrom(project).map { it.kotlinFile }
    return kotlinToJSTranslator.translate(files, project.args.split(" "))
  }

  fun complete(project: Project, line: Int, character: Int): List<Completion> {
    val file = getFilesFrom(project).first()
    return try {
      val isJs = project.confType == ProjectType.JS
      completionProvider.complete(file, line, character, isJs)
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

  fun getVersion() = version

  private fun getFilesFrom(project: Project) = project.files.map {
    KotlinFile.from(kotlinEnvironment.coreEnvironment.project, it.name, it.text)
  }
}