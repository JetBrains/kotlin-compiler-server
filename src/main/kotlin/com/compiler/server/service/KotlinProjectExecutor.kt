package com.compiler.server.service

import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.components.*
import com.compiler.server.model.*
import com.compiler.server.model.bean.VersionInfo
import org.apache.commons.logging.LogFactory
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.springframework.stereotype.Component

@Component
class KotlinProjectExecutor(
  private val kotlinCompiler: KotlinCompiler,
  private val suggestionProvider: SuggestionProvider,
  private val errorAnalyzer: ErrorAnalyzer,
  private val version: VersionInfo,
  private val kotlinToJSTranslator: KotlinToJSTranslator,
  private val kotlinEnvironment: KotlinEnvironment
) {

  private val log = LogFactory.getLog(KotlinProjectExecutor::class.java)

  fun run(project: Project): ExecutionResult {
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      kotlinCompiler.run(files, environment, project.args)
    }
  }

  fun test(project: Project): ExecutionResult {
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      kotlinCompiler.test(files, environment)
    }
  }

  fun convertToJs(project: Project): TranslationJSResult {
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      kotlinToJSTranslator.translate(files, project.args.split(" "), environment)
    }
  }

  fun complete(project: Project, line: Int, character: Int): List<Completion> {
    return kotlinEnvironment.environment {
      val file = getFilesFrom(project, it).first()
      try {
        val isJs = project.confType == ProjectType.JS
        suggestionProvider.complete(file, line, character, isJs, it)
      } catch (e: Exception) {
        log.warn("Exception in getting completions. Project: $project", e)
        emptyList()
      }
    }
  }

  fun completeWithImport(project: Project, line: Int, character: Int) : List<ImportInfo> {
    return kotlinEnvironment.environment {
      val file = getFilesFrom(project, it).first()
      try {
        val isJs = project.confType == ProjectType.JS
        suggestionProvider.completeWithImport(file, line, character, isJs, it)
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
        errorAnalyzer.errorsFrom(files, environment).errors
      } catch (e: Exception) {
        log.warn("Exception in getting highlight. Project: $project", e)
        emptyMap()
      }
    }
  }

  fun highlightWithImports(project: Project): Map<String, List<ErrorDescriptor>> {
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      try {
        val res = errorAnalyzer.errorsFrom(files, environment).errors
        return@environment suggestionProvider.checkUnresolvedReferences(res)
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
}