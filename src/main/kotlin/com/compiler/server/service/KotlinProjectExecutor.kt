package com.compiler.server.service

import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.components.*
import com.compiler.server.model.*
import com.compiler.server.model.bean.VersionInfo
import com.compiler.server.utils.LoggerHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class KotlinProjectExecutor(
  private val kotlinCompiler: KotlinCompiler,
  private val completionProvider: CompletionProvider,
  private val errorAnalyzer: ErrorAnalyzer,
  private val version: VersionInfo,
  private val kotlinToJSTranslator: KotlinToJSTranslator,
  private val kotlinEnvironment: KotlinEnvironment,
  @Value("\${executor.logs}") private val executorLogs: Boolean
) {

  private val log = LoggerFactory.getLogger(KotlinProjectExecutor::class.java)

  fun run(project: Project): ExecutionResult {
    val files = getFilesFrom(project).map { it.kotlinFile }
    return kotlinCompiler.run(files, project.args).also { logExecutionResult(project, it) }
  }

  fun test(project: Project): ExecutionResult {
    val files = getFilesFrom(project).map { it.kotlinFile }
    return kotlinCompiler.test(files).also { logExecutionResult(project, it) }
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
      log.warn("Exception in getting completions. Project: $project", e)
      emptyList()
    }
  }

  fun highlight(project: Project): Map<String, List<ErrorDescriptor>> {
    val files = getFilesFrom(project).map { it.kotlinFile }
    return try {
      errorAnalyzer.errorsFrom(files)
    }
    catch (e: Exception) {
      log.warn("Exception in getting highlight. Project: $project", e)
      emptyMap()
    }
  }

  fun getVersion() = version

  private fun logExecutionResult(project: Project, executionResult: ExecutionResult) {
    if (executorLogs.not()) return
    LoggerHelper.logUnsuccessfulExecutionResult(
      executionResult,
      project.confType,
      getVersion().version
    )
  }

  private fun getFilesFrom(project: Project) = project.files.map {
    KotlinFile.from(kotlinEnvironment.coreEnvironment.project, it.name, it.text)
  }
}