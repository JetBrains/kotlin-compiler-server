package com.compiler.server.service

import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.components.*
import com.compiler.server.model.*
import common.model.Completion
import com.compiler.server.model.bean.VersionInfo
import component.KotlinEnvironment
import net.logstash.logback.marker.Markers
import org.slf4j.LoggerFactory
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
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
    }.also { onExecutionResult(project, it) }
  }

  fun test(project: Project): ExecutionResult {
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      kotlinCompiler.test(files, environment)
    }.also { onExecutionResult(project, it) }
  }

  fun convertToJs(project: Project): TranslationJSResult {
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      kotlinToJSTranslator.translate(
        files,
        project.args.split(" "),
        environment,
        kotlinToJSTranslator::doTranslate
      )
    }.also { onExecutionResult(project, it) }
  }

  fun convertToJsIr(project: Project): TranslationJSResult {
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      kotlinToJSTranslator.translate(
        files,
        project.args.split(" "),
        environment,
        kotlinToJSTranslator::doTranslateWithIr
      )
    }.also { onExecutionResult(project, it) }
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

  private fun getFilesFrom(project: Project, coreEnvironment: KotlinCoreEnvironment) = project.files.map {
    KotlinFile.from(coreEnvironment.project, it.name, it.text)
  }

  private fun onExecutionResult(project: Project, executionResult: ExecutionResult) {
    if (executorLogs && executionResult.isUnsuccessful()) {
      log.info(
        Markers.appendFields(
          UnsuccessfulExecutionDetails(
            executionResult.errorsMessages(),
            project.confType.toString(),
            getVersion().version
          )
        ), "Execution is unsuccessful."
      )
    }
  }
}

data class UnsuccessfulExecutionDetails(
  val executionErrors: List<String>,
  val confType: String,
  val kotlinVersion: String
)