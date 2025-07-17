package com.compiler.server.service

import com.compiler.server.common.components.KotlinEnvironment
import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.components.*
import com.compiler.server.model.*
import com.compiler.server.model.bean.VersionInfo
import model.Completion
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.psi.KtFile
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

@Component
class KotlinProjectExecutor(
  private val kotlinCompiler: KotlinCompiler,
  private val completionProvider: CompletionProvider,
  private val version: VersionInfo,
  private val kotlinToJSTranslator: KotlinToJSTranslator,
  private val kotlinEnvironment: KotlinEnvironment,
  private val loggerDetailsStreamer: LoggerDetailsStreamer? = null,
) {

  private val log = LoggerFactory.getLogger(KotlinProjectExecutor::class.java)

  fun run(project: Project, addByteCode: Boolean): ExecutionResult {
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      kotlinCompiler.run(files, addByteCode, project.args)
    }.also { logExecutionResult(project, it) }
  }

  fun test(project: Project, addByteCode: Boolean): ExecutionResult {
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      kotlinCompiler.test(files, addByteCode)
    }.also { logExecutionResult(project, it) }
  }

  fun convertToJsIr(project: Project): TranslationJSResult {
    return convertJsWithConverter(project, kotlinToJSTranslator::doTranslateWithIr)
  }

  fun compileToJvm(project: Project): CompilationResult<KotlinCompiler.JvmClasses> {
    val files = kotlinEnvironment.environment { environment ->
      getFilesFrom(project, environment).map { it.kotlinFile }
    }
    return kotlinCompiler.compile(files)
  }

  fun convertToWasm(
    project: Project,
    debugInfo: Boolean,
    multiModule: Boolean,
  ): TranslationResultWithJsCode {
    return convertWasmWithConverter(
      project,
      debugInfo,
      multiModule,
      kotlinToJSTranslator::doTranslateWithWasm
    )
  }

  fun complete(project: Project, line: Int, character: Int): List<Completion> {
    return kotlinEnvironment.environment {
      val file = getFilesFrom(project, it).first()
      try {
        completionProvider.complete(file, line, character, project.confType, it)
      } catch (e: Exception) {
        log.warn("Exception in getting completions. Project: $project", e)
        emptyList()
      }
    }
  }

  fun highlight(project: Project): CompilerDiagnostics = try {
    when (project.confType) {
      ProjectType.JAVA, ProjectType.JUNIT -> compileToJvm(project).compilerDiagnostics
      ProjectType.CANVAS, ProjectType.JS, ProjectType.JS_IR ->
        convertToJsIr(
          project,
        ).compilerDiagnostics
      ProjectType.WASM -> convertToWasm(
        project,
        debugInfo = false,
        multiModule = false,
      ).compilerDiagnostics
      ProjectType.COMPOSE_WASM -> convertToWasm(
        project,
        debugInfo = false,
        multiModule = true,
      ).compilerDiagnostics
    }
  } catch (e: Exception) {
    log.warn("Exception in getting highlight. Project: $project", e)
    CompilerDiagnostics(emptyMap())
  }

  fun getVersion() = version

  private fun convertJsWithConverter(
    project: Project,
    converter: (List<KtFile>, List<String>) -> CompilationResult<String>
  ): TranslationJSResult {
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      kotlinToJSTranslator.translateJs(
        files,
        project.args.split(" "),
        converter
      )
    }.also { logExecutionResult(project, it) }
  }

  private fun convertWasmWithConverter(
    project: Project,
    debugInfo: Boolean,
    multiModule: Boolean,
    converter: (
      List<KtFile>,
      List<String>,
      List<String>,
      List<String>,
      Boolean,
      Boolean,
    ) -> CompilationResult<WasmTranslationSuccessfulOutput>
  ): TranslationResultWithJsCode {
    return kotlinEnvironment.environment { environment ->
      val files = getFilesFrom(project, environment).map { it.kotlinFile }
      kotlinToJSTranslator.translateWasm(
        files,
        debugInfo,
        multiModule,
        project.confType,
        converter
      )
    }.also { logExecutionResult(project, it) }
  }

  private fun logExecutionResult(project: Project, executionResult: ExecutionResult) {
    loggerDetailsStreamer?.logExecutionResult(
      executionResult,
      project.confType,
      getVersion().version
    )
  }

  private fun getFilesFrom(project: Project, coreEnvironment: KotlinCoreEnvironment) = project.files.map {
    KotlinFile.from(coreEnvironment.project, it.name, it.text)
  }
}
