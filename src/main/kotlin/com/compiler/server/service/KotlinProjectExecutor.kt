package com.compiler.server.service

import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.components.CompletionProvider
import com.compiler.server.compiler.components.ErrorAnalyzer
import com.compiler.server.compiler.components.KotlinCompiler
import com.compiler.server.compiler.components.KotlinToJSTranslator
import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.Project
import com.compiler.server.model.TranslationJSResult
import com.compiler.server.model.bean.VersionInfo
import com.compiler.server.utils.LoggerHelper
import component.KotlinEnvironment
import model.Completion
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.psi.KtFile
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
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      kotlinCompiler.run(files, environment, project.args)
    }.also { logExecutionResult(project, it) }
  }

  fun test(project: Project): ExecutionResult {
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      kotlinCompiler.test(files, environment)
    }.also { logExecutionResult(project, it) }
  }

  fun convertToJs(project: Project): TranslationJSResult {
    return convertJsWithConverter(project, kotlinToJSTranslator::doTranslate)
  }

  fun convertToJsIr(project: Project): TranslationJSResult {
    return convertJsWithConverter(project, kotlinToJSTranslator::doTranslateWithIr)
  }

  fun complete(project: Project, line: Int, character: Int): List<Completion> {
    return kotlinEnvironment.environment {
      val file = getFilesFrom(project, it).first()
      try {
        val isJs = project.confType.isJsRelated()
        completionProvider.complete(file, line, character, isJs, it)
      } catch (e: Exception) {
        log.warn("Exception in getting completions. Project: $project", e)
        emptyList()
      }
    }
  }

  fun highlight(project: Project): Map<String, List<ErrorDescriptor>> {
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      try {
        val isJs = project.confType.isJsRelated()
        errorAnalyzer.errorsFrom(
          files = files,
          coreEnvironment = environment,
          isJs = isJs
        ).errors
      } catch (e: Exception) {
        log.warn("Exception in getting highlight. Project: $project", e)
        emptyMap()
      }
    }
  }

  fun getVersion() = version

  private fun convertJsWithConverter(
    project: Project,
    converter: (List<KtFile>, List<String>, KotlinCoreEnvironment) -> TranslationJSResult
  ): TranslationJSResult {
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      kotlinToJSTranslator.translate(
        files,
        project.args.split(" "),
        environment,
        converter
      )
    }.also { logExecutionResult(project, it) }
  }

  private fun logExecutionResult(project: Project, executionResult: ExecutionResult) {
    if (executorLogs.not()) return
    LoggerHelper.logExecutionResult(
      executionResult,
      project.confType,
      getVersion().version
    )
  }

  private fun getFilesFrom(project: Project, coreEnvironment: KotlinCoreEnvironment) = project.files.map {
    KotlinFile.from(coreEnvironment.project, it.name, it.text)
  }
}