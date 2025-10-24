package com.compiler.server.compiler.components

import com.compiler.server.model.*
import com.compiler.server.utils.CompilerArgumentsUtil
import com.compiler.server.utils.JS_DEFAULT_MODULE_NAME
import com.compiler.server.utils.WASM_DEFAULT_MODULE_NAME
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.springframework.stereotype.Service
import kotlin.io.path.*

@Service
class KotlinToJSTranslator(
    private val compilerArgumentsUtil: CompilerArgumentsUtil,
    private val jsCompilerArguments: Set<ExtendedCompilerArgument>,
    private val wasmCompilerArguments: Set<ExtendedCompilerArgument>,
    private val composeWasmCompilerArguments: Set<ExtendedCompilerArgument>
) {
    companion object {
        internal const val JS_IR_CODE_BUFFER = "playground.output?.buffer_1;\n"

        internal val JS_IR_OUTPUT_REWRITE = """
        if (typeof get_output !== "undefined") {
          get_output();
          output = new BufferedOutput();
          _.output = get_output();
        }
        """.trimIndent()

        const val BEFORE_MAIN_CALL_LINE = 4
    }

    fun translateJs(
        files: List<ProjectFile>,
        arguments: List<String>,
        jsCompilerArguments: JsCompilerArguments,
        translate: (List<ProjectFile>, List<String>, JsCompilerArguments) -> CompilationResult<String>
    ): TranslationJSResult = try {
        val compilationResult = translate(files, arguments, jsCompilerArguments)
        val jsCode = when (compilationResult) {
            is Compiled<String> -> compilationResult.result
            is NotCompiled -> null
        }
        TranslationJSResult(jsCode = jsCode, compilerDiagnostics = compilationResult.compilerDiagnostics)
    } catch (e: Exception) {
        TranslationJSResult(exception = e.toExceptionDescriptor())
    }

    fun translateWasm(
        files: List<ProjectFile>,
        debugInfo: Boolean,
        projectType: ProjectType,
        userCompilerArguments: JsCompilerArguments,
        translate: (List<ProjectFile>, ProjectType, Boolean, JsCompilerArguments) -> CompilationResult<WasmTranslationSuccessfulOutput>
    ): TranslationResultWithJsCode {
        return try {
            val compilationResult = translate(
                files, projectType, debugInfo, userCompilerArguments
            )
            val wasmCompilationOutput = when (compilationResult) {
                is Compiled<WasmTranslationSuccessfulOutput> -> compilationResult.result
                is NotCompiled -> return TranslationJSResult(compilerDiagnostics = compilationResult.compilerDiagnostics)
            }
            TranslationWasmResult(
                jsCode = wasmCompilationOutput.jsCode,
                jsInstantiated = wasmCompilationOutput.jsInstantiated,
                compilerDiagnostics = compilationResult.compilerDiagnostics,
                wasm = wasmCompilationOutput.wasm,
                wat = if (debugInfo) wasmCompilationOutput.wat else null
            )
        } catch (e: Exception) {
            TranslationJSResult(exception = e.toExceptionDescriptor())
        }
    }

    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalCompilerArgument::class)
    fun doTranslateWithIr(
        files: List<ProjectFile>, arguments: List<String>, userCompilerArguments: JsCompilerArguments
    ): CompilationResult<String> = usingTempDirectory { inputDir ->
        usingTempDirectory { outputDir ->
            files.writeToIoFiles(inputDir)
            val klibPath = (outputDir / "klib").toFile().canonicalPath
            val additionalCompilerArgumentsForKLib = compilerArgumentsUtil.convertCompilerArgumentsToCompilationString(
                jsCompilerArguments,
                compilerArgumentsUtil.PREDEFINED_JS_FIRST_PHASE_ARGUMENTS,
                userCompilerArguments.firstPhase
            ) + "-ir-output-dir=$klibPath"


            val sources = inputDir.listDirectoryEntries()

            val logger = CompilationLogger()
            logger.compilationLogs = sources.filter { it.name.endsWith(".kt") }.associate { it.name to mutableListOf() }

            val toolchains = KotlinToolchains.loadImplementation(ClassLoader.getSystemClassLoader())
            val jsToolchain = toolchains.getToolchain(JsPlatformToolchain::class.java)

            toolchains.createBuildSession().use { session ->
                val result = try {
                    val operation = jsToolchain.createJsCompilationOperation(sources, outputDir)
                    operation.compilerArguments.applyArgumentStrings(arguments + additionalCompilerArgumentsForKLib)

                    session.executeOperation(operation, toolchains.createInProcessExecutionPolicy(), logger)
                } catch (e: Exception) {
                    throw Exception("Exception executing compilation operation", e)
                }
                return@use toCompilationResult(result, logger).flatMap {
                    val secondPhaseArguments = compilerArgumentsUtil.convertCompilerArgumentsToCompilationString(
                        jsCompilerArguments,
                        compilerArgumentsUtil.PREDEFINED_JS_SECOND_PHASE_ARGUMENTS,
                        userCompilerArguments.secondPhase
                    ) + "-ir-output-dir=${(outputDir / "js").toFile().canonicalPath}" + "-Xinclude=$klibPath"

                    val result = try {
                        val operation = jsToolchain.createJsCompilationOperation(sources, outputDir)
                        operation.compilerArguments.applyArgumentStrings(secondPhaseArguments)

                        session.executeOperation(operation, toolchains.createInProcessExecutionPolicy(), logger)
                    } catch (e: Exception) {
                        throw Exception("Exception executing compilation operation", e)
                    }
                    toCompilationResultString(result, logger)

                }.map { (outputDir / "js" / "$JS_DEFAULT_MODULE_NAME.js").readText() }
                    .map { it.withMainArgumentsIr(arguments) }.map(::redirectOutput)
            }

//            k2JSCompiler.tryCompilation(inputDir, ioFiles, filePaths + additionalCompilerArgumentsForKLib).flatMap {
//                    val secondPhaseArguments = compilerArgumentsUtil.convertCompilerArgumentsToCompilationString(
//                        jsCompilerArguments,
//                        compilerArgumentsUtil.PREDEFINED_JS_SECOND_PHASE_ARGUMENTS,
//                        userCompilerArguments.secondPhase
//                    ) + "-ir-output-dir=${(outputDir / "js").toFile().canonicalPath}" + "-Xinclude=$klibPath"
//                    k2JSCompiler.tryCompilation(inputDir, ioFiles, secondPhaseArguments)
//                }.map { (outputDir / "js" / "$JS_DEFAULT_MODULE_NAME.js").readText() }
//                .map { it.withMainArgumentsIr(arguments) }.map(::redirectOutput)
        }
    }

    private fun toCompilationResult(
        buildResult: org.jetbrains.kotlin.buildtools.api.CompilationResult,
        logger: CompilationLogger,
    ): CompilationResult<Unit> = when (buildResult) {
        org.jetbrains.kotlin.buildtools.api.CompilationResult.COMPILATION_SUCCESS -> {
            val compilerDiagnostics = CompilerDiagnostics(logger.compilationLogs)
//            val outputFiles = buildMap {
//                outputDir.visitFileTree {
//                    onVisitFile { file, _ ->
//                        put(file.relativeTo(outputDir).pathString, file.readBytes())
//                        FileVisitResult.CONTINUE
//                    }
//                }
//            }
            Compiled(result = Unit, compilerDiagnostics = compilerDiagnostics)

//            Compiled(
//                compilerDiagnostics = compilerDiagnostics,
//                result = JvmClasses(
//                    files = outputFiles,
//                )
//            )
        }

        else -> NotCompiled(CompilerDiagnostics(logger.compilationLogs))
    }

    private fun toCompilationResultString(
        buildResult: org.jetbrains.kotlin.buildtools.api.CompilationResult,
        logger: CompilationLogger,
    ): CompilationResult<String> = when (buildResult) {
        org.jetbrains.kotlin.buildtools.api.CompilationResult.COMPILATION_SUCCESS -> {
            val compilerDiagnostics = CompilerDiagnostics(logger.compilationLogs)
//            val outputFiles = buildMap {
//                outputDir.visitFileTree {
//                    onVisitFile { file, _ ->
//                        put(file.relativeTo(outputDir).pathString, file.readBytes())
//                        FileVisitResult.CONTINUE
//                    }
//                }
//            }
            Compiled(result = "", compilerDiagnostics = compilerDiagnostics)

//            Compiled(
//                compilerDiagnostics = compilerDiagnostics,
//                result = JvmClasses(
//                    files = outputFiles,
//                )
//            )
        }

        else -> NotCompiled(CompilerDiagnostics(logger.compilationLogs))
    }

    private fun redirectOutput(code: String): String {
        val listLines = code.lineSequence().toMutableList()

        listLines.add(listLines.size - BEFORE_MAIN_CALL_LINE, JS_IR_OUTPUT_REWRITE)
        listLines.add(listLines.size - 1, JS_IR_CODE_BUFFER)
        return listLines.joinToString("\n")
    }


    fun doTranslateWithWasm(
        files: List<ProjectFile>,
        projectType: ProjectType,
        debugInfo: Boolean,
        userCompilerArguments: JsCompilerArguments
    ): CompilationResult<WasmTranslationSuccessfulOutput> = usingTempDirectory { inputDir ->
        usingTempDirectory { outputDir ->
            val (defaultCompilerArgs, firstPhasePredefinedArguments, secondPhasePredefinedArguments) = when (projectType) {
                ProjectType.WASM -> Triple(
                    wasmCompilerArguments,
                    compilerArgumentsUtil.PREDEFINED_WASM_FIRST_PHASE_ARGUMENTS,
                    compilerArgumentsUtil.PREDEFINED_WASM_SECOND_PHASE_ARGUMENTS,
                )

                ProjectType.COMPOSE_WASM -> Triple(
                    composeWasmCompilerArguments,
                    compilerArgumentsUtil.PREDEFINED_COMPOSE_WASM_FIRST_PHASE_ARGUMENTS,
                    compilerArgumentsUtil.PREDEFINED_COMPOSE_WASM_SECOND_PHASE_ARGUMENTS
                )

                else -> throw IllegalStateException("Wasm should have wasm or compose-wasm project type")
            }
            val ioFiles = files.writeToIoFiles(inputDir)
            val k2JSCompiler = K2JSCompiler()
            val filePaths = ioFiles.map { it.toFile().canonicalPath }
            val klibPath = (outputDir / "klib").toFile().canonicalPath

            val additionalCompilerArgumentsForKLib = compilerArgumentsUtil.convertCompilerArgumentsToCompilationString(
                defaultCompilerArgs, firstPhasePredefinedArguments, userCompilerArguments.firstPhase
            ) + "-ir-output-dir=$klibPath"

            k2JSCompiler.tryCompilation(inputDir, ioFiles, filePaths + additionalCompilerArgumentsForKLib).flatMap {
                    val secondPhaseArguments = (compilerArgumentsUtil.convertCompilerArgumentsToCompilationString(
                        defaultCompilerArgs, secondPhasePredefinedArguments, userCompilerArguments.secondPhase
                    ) + "-ir-output-dir=${(outputDir / "wasm").toFile().canonicalPath}" + "-Xinclude=$klibPath").toMutableList()

                    if (debugInfo) secondPhaseArguments.add("-Xwasm-generate-wat")

                    k2JSCompiler.tryCompilation(inputDir, ioFiles, secondPhaseArguments)
                }.map {
                    WasmTranslationSuccessfulOutput(
                        jsCode = (outputDir / "wasm" / "$WASM_DEFAULT_MODULE_NAME.uninstantiated.mjs").readText(),
                        jsInstantiated = (outputDir / "wasm" / "$WASM_DEFAULT_MODULE_NAME.mjs").readText(),
                        wasm = (outputDir / "wasm" / "$WASM_DEFAULT_MODULE_NAME.wasm").readBytes(),
                        wat = if (debugInfo) (outputDir / "wasm" / "$WASM_DEFAULT_MODULE_NAME.wat").readText() else null,
                    )
                }
        }
    }
}

private fun String.withMainArgumentsIr(arguments: List<String>): String {
    val mainIrFunction = """
    |  function mainWrapper() {
    |    main([%s]);
    |  }
  """.trimMargin()

    return replace(
        String.format(mainIrFunction, ""),
        String.format(mainIrFunction, arguments.joinToString { ObjectMapper().writeValueAsString(it) })
    )
}

data class WasmTranslationSuccessfulOutput(
    val jsCode: String,
    val jsInstantiated: String,
    val wasm: ByteArray,
    val wat: String?,
)
