package com.compiler.server.compiler.components

import com.compiler.server.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import component.KotlinEnvironment
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.springframework.stereotype.Service
import kotlin.io.path.div
import kotlin.io.path.readBytes
import kotlin.io.path.readText

@Service
class KotlinToJSTranslator(
    private val kotlinEnvironment: KotlinEnvironment,
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
        translate: (List<ProjectFile>, List<String>) -> CompilationResult<String>
    ): TranslationJSResult = try {
        val compilationResult = translate(files, arguments)
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
        translate: (List<ProjectFile>, List<String>, List<String>, List<String>, Boolean) -> CompilationResult<WasmTranslationSuccessfulOutput>
    ): TranslationResultWithJsCode {
        return try {
            val (dependencies, compilerPlugins, compilerPluginOptions) = when (projectType) {
                ProjectType.WASM -> listOf(
                    kotlinEnvironment.WASM_LIBRARIES,
                    emptyList(),
                    emptyList()
                )

                ProjectType.COMPOSE_WASM -> listOf(
                    kotlinEnvironment.COMPOSE_WASM_LIBRARIES,
                    kotlinEnvironment.COMPOSE_WASM_COMPILER_PLUGINS,
                    kotlinEnvironment.composeWasmCompilerPluginOptions
                )

                else -> throw IllegalStateException("Wasm should have wasm or compose-wasm project type")
            }
            val compilationResult = translate(
                files,
                dependencies,
                compilerPlugins,
                compilerPluginOptions,
                debugInfo
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

    fun doTranslateWithIr(files: List<ProjectFile>, arguments: List<String>): CompilationResult<String> =
        usingTempDirectory { inputDir ->
            val moduleName = "playground"
            usingTempDirectory { outputDir ->
                val ioFiles = files.writeToIoFiles(inputDir)
                val k2JSCompiler = K2JSCompiler()
                val filePaths = ioFiles.map { it.toFile().canonicalPath }
                val klibPath = (outputDir / "klib").toFile().canonicalPath
                val additionalCompilerArgumentsForKLib = listOf(
                    "-Xreport-all-warnings",
                    "-Wextra",
                    "-Xir-produce-klib-dir",
                    "-libraries=${kotlinEnvironment.JS_LIBRARIES.joinToString(PATH_SEPARATOR)}",
                    "-ir-output-dir=$klibPath",
                    "-ir-output-name=$moduleName",
                )
                k2JSCompiler.tryCompilation(inputDir, ioFiles, filePaths + additionalCompilerArgumentsForKLib)
                    .flatMap {
                        k2JSCompiler.tryCompilation(
                            inputDir, ioFiles, listOf(
                                "-Xreport-all-warnings",
                                "-Wextra",
                                "-Xir-produce-js",
                                "-Xir-dce",
                                "-Xinclude=$klibPath",
                                "-libraries=${kotlinEnvironment.JS_LIBRARIES.joinToString(PATH_SEPARATOR)}",
                                "-ir-output-dir=${(outputDir / "js").toFile().canonicalPath}",
                                "-ir-output-name=$moduleName",
                            )
                        )
                    }
                    .map { (outputDir / "js" / "$moduleName.js").readText() }
                    .map { it.withMainArgumentsIr(arguments) }
                    .map(::redirectOutput)
            }
        }

    private fun redirectOutput(code: String): String {
        val listLines = code
            .lineSequence()
            .toMutableList()

        listLines.add(listLines.size - BEFORE_MAIN_CALL_LINE, JS_IR_OUTPUT_REWRITE)
        listLines.add(listLines.size - 1, JS_IR_CODE_BUFFER)
        return listLines.joinToString("\n")
    }


    fun doTranslateWithWasm(
        files: List<ProjectFile>,
        dependencies: List<String>,
        compilerPlugins: List<String>,
        compilerPluginOptions: List<String>,
        debugInfo: Boolean,
    ): CompilationResult<WasmTranslationSuccessfulOutput> =
        usingTempDirectory { inputDir ->
            val moduleName = "playground"
            usingTempDirectory { outputDir ->
                val ioFiles = files.writeToIoFiles(inputDir)
                val k2JSCompiler = K2JSCompiler()
                val filePaths = ioFiles.map { it.toFile().canonicalPath }
                val klibPath = (outputDir / "klib").toFile().canonicalPath
                val compilerPluginsArgs: List<String> = compilerPlugins
                    .takeIf { it.isNotEmpty() }
                    ?.let { plugins ->
                        plugins.map {
                            "-Xplugin=$it"
                        } + compilerPluginOptions.map {
                            "-P=$it"
                        }
                    } ?: emptyList()
                val additionalCompilerArgumentsForKLib: List<String> = listOf(
                    "-Xreport-all-warnings",
                    "-Wextra",
                    "-Xwasm",
                    "-Xir-produce-klib-dir",
                    "-libraries=${dependencies.joinToString(PATH_SEPARATOR)}",
                    "-ir-output-dir=$klibPath",
                    "-ir-output-name=$moduleName",
                ) + compilerPluginsArgs

                k2JSCompiler.tryCompilation(inputDir, ioFiles, filePaths + additionalCompilerArgumentsForKLib)
                    .flatMap {
                        k2JSCompiler.tryCompilation(
                            inputDir, ioFiles, mutableListOf(
                                "-Xreport-all-warnings",
                                "-Wextra",
                                "-Xwasm",
                                "-Xir-produce-js",
                                "-Xir-dce",
                                "-Xinclude=$klibPath",
                                "-libraries=${dependencies.joinToString(PATH_SEPARATOR)}",
                                "-ir-output-dir=${(outputDir / "wasm").toFile().canonicalPath}",
                                "-ir-output-name=$moduleName",
                            ).also { if (debugInfo) it.add("-Xwasm-generate-wat") })
                    }
                    .map {
                        WasmTranslationSuccessfulOutput(
                            jsCode = (outputDir / "wasm" / "$moduleName.uninstantiated.mjs").readText(),
                            jsInstantiated = (outputDir / "wasm" / "$moduleName.mjs").readText(),
                            wasm = (outputDir / "wasm" / "$moduleName.wasm").readBytes(),
                            wat = if (debugInfo) (outputDir / "wasm" / "$moduleName.wat").readText() else null,
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
