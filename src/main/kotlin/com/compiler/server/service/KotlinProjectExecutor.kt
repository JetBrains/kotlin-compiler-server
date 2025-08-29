package com.compiler.server.service

import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.components.CompilationResult
import com.compiler.server.compiler.components.CompletionProvider
import com.compiler.server.compiler.components.KotlinCompiler
import com.compiler.server.compiler.components.KotlinToJSTranslator
import com.compiler.server.compiler.components.LoggerDetailsStreamer
import com.compiler.server.compiler.components.WasmTranslationSuccessfulOutput
import com.compiler.server.model.CompilerDiagnostics
import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.JsCompilerArguments
import com.compiler.server.model.Project
import com.compiler.server.model.ProjectType
import com.compiler.server.model.TranslationJSResult
import com.compiler.server.model.TranslationResultWithJsCode
import com.compiler.server.model.bean.VersionInfo
import component.KotlinEnvironment
import model.Completion
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.psi.KtFile
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

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
            kotlinCompiler.run(files, addByteCode, project.args, project.compilerArguments.getOrElse(0, { emptyMap() }))
        }.also { logExecutionResult(project, it) }
    }

    fun test(project: Project, addByteCode: Boolean): ExecutionResult {
        return kotlinEnvironment.environment { environment ->
            val files = getFilesFrom(project, environment).map { it.kotlinFile }
            kotlinCompiler.test(files, addByteCode, project.compilerArguments.getOrElse(0, { emptyMap() }))
        }.also { logExecutionResult(project, it) }
    }

    fun convertToJsIr(project: Project): TranslationJSResult {
        return convertJsWithConverter(project, kotlinToJSTranslator::doTranslateWithIr)
    }

    fun compileToJvm(project: Project): CompilationResult<KotlinCompiler.JvmClasses> {
        val files = kotlinEnvironment.environment { environment ->
            getFilesFrom(project, environment).map { it.kotlinFile }
        }
        return kotlinCompiler.compile(files, project.compilerArguments.getOrElse(0, { emptyMap() }))
    }

    fun convertToWasm(project: Project, debugInfo: Boolean = false): TranslationResultWithJsCode {
        return convertWasmWithConverter(project, debugInfo, kotlinToJSTranslator::doTranslateWithWasm)
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

            ProjectType.WASM, ProjectType.COMPOSE_WASM ->
                convertToWasm(
                    project,
                    debugInfo = false,
                ).compilerDiagnostics
        }
    } catch (e: Exception) {
        log.warn("Exception in getting highlight. Project: $project", e)
        CompilerDiagnostics(emptyMap())
    }

    fun getVersion() = version

    private fun convertJsWithConverter(
        project: Project,
        converter: (List<KtFile>, List<String>, JsCompilerArguments) -> CompilationResult<String>
    ): TranslationJSResult {
        return kotlinEnvironment.environment { environment ->
            val files = getFilesFrom(project, environment).map { it.kotlinFile }
            kotlinToJSTranslator.translateJs(
                files,
                project.args.split(" "),
                JsCompilerArguments(
                    project.compilerArguments.getOrElse(0, { emptyMap() }),
                    project.compilerArguments.getOrElse(1, { emptyMap() })
                ),
                converter,
            )
        }.also { logExecutionResult(project, it) }
    }

    private fun convertWasmWithConverter(
        project: Project,
        debugInfo: Boolean,
        converter: (List<KtFile>, ProjectType, Boolean, JsCompilerArguments) -> CompilationResult<WasmTranslationSuccessfulOutput>
    ): TranslationResultWithJsCode {
        return kotlinEnvironment.environment { environment ->
            val files = getFilesFrom(project, environment).map { it.kotlinFile }
            kotlinToJSTranslator.translateWasm(
                files,
                debugInfo,
                project.confType,
                JsCompilerArguments(
                    project.compilerArguments.getOrElse(0, { emptyMap() }),
                    project.compilerArguments.getOrElse(1, { emptyMap() })
                ),
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
