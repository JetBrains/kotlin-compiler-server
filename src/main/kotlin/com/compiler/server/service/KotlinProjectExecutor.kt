package com.compiler.server.service

import com.compiler.server.compiler.components.*
import com.compiler.server.model.*
import com.compiler.server.model.bean.VersionInfo
import model.Completion
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KotlinProjectExecutor(
  private val kotlinCompiler: KotlinCompiler,
  private val version: VersionInfo,
  private val kotlinToJSTranslator: KotlinToJSTranslator,
  private val loggerDetailsStreamer: LoggerDetailsStreamer? = null,
) {

  private val log = LoggerFactory.getLogger(KotlinProjectExecutor::class.java)

  fun run(project: Project): ExecutionResult {
    return kotlinCompiler.run(project.files, project.args).also { logExecutionResult(project, it) }
  }

  fun test(project: Project): ExecutionResult {
    return kotlinCompiler.test(project.files).also { logExecutionResult(project, it) }
  }

  fun convertToJsIr(project: Project): TranslationJSResult {
    return convertJsWithConverter(project, kotlinToJSTranslator::doTranslateWithIr)
  }

  fun compileToJvm(project: Project): CompilationResult<KotlinCompiler.JvmClasses> {
    return kotlinCompiler.compile(project.files)
  }

  fun convertToWasm(project: Project): TranslationResultWithJsCode {
    return convertWasmWithConverter(project, kotlinToJSTranslator::doTranslateWithWasm)
  }

  @Suppress("unused")
  fun complete(project: Project, line: Int, character: Int): List<Completion> {
    return emptyList()
  }

  fun highlight(project: Project): CompilerDiagnostics = try {
    when (project.confType) {
      ProjectType.JAVA, ProjectType.JUNIT -> compileToJvm(project).compilerDiagnostics
      ProjectType.CANVAS, ProjectType.JS, ProjectType.JS_IR -> convertToJsIr(project).compilerDiagnostics
      ProjectType.WASM -> convertToWasm(project).compilerDiagnostics
    }
  } catch (e: Exception) {
    log.warn("Exception in getting highlight. Project: $project", e)
    CompilerDiagnostics(emptyMap())
  }

  fun getVersion() = version

  private fun convertJsWithConverter(
    project: Project,
    converter: (List<ProjectFile>, List<String>) -> CompilationResult<String>
  ): TranslationJSResult {
    return kotlinToJSTranslator.translateJs(project.files, project.args.split(" "), converter)
      .also { logExecutionResult(project, it) }
  }

  private fun convertWasmWithConverter(
    project: Project,
    converter: (List<ProjectFile>) -> CompilationResult<WasmTranslationSuccessfulOutput>
  ): TranslationResultWithJsCode {
    return kotlinToJSTranslator.translateWasm(project.files, converter)
      .also { logExecutionResult(project, it) }
  }

  private fun logExecutionResult(project: Project, executionResult: ExecutionResult) {
    loggerDetailsStreamer?.logExecutionResult(
      executionResult,
      project.confType,
      getVersion().version
    )
  }
}
