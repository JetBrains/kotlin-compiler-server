package com.compiler.server.compiler.components

import com.compiler.server.model.ExtendedCompilerArgument
import com.compiler.server.model.JsCompilerArguments
import com.compiler.server.model.ProjectFile
import com.compiler.server.model.ProjectType
import com.compiler.server.model.TranslationJSResult
import com.compiler.server.model.TranslationResultWithJsCode
import com.compiler.server.model.TranslationWasmResult
import com.compiler.server.model.toExceptionDescriptor
import com.compiler.server.utils.CompilerArgumentsUtil
import com.compiler.server.utils.JS_DEFAULT_MODULE_NAME
import com.compiler.server.utils.WASM_DEFAULT_MODULE_NAME
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.springframework.stereotype.Service
import kotlin.io.encoding.Base64
import kotlin.io.path.div
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.readText

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
                files,
                projectType,
                debugInfo,
                userCompilerArguments
            )
            val wasmCompilationOutput = when (compilationResult) {
                is Compiled<WasmTranslationSuccessfulOutput> -> compilationResult.result
                is NotCompiled -> return TranslationJSResult(compilerDiagnostics = compilationResult.compilerDiagnostics)
            }
            val jsCode = convertOutputToJsCode(wasmCompilationOutput)
            TranslationWasmResult(jsCode = jsCode, deps = wasmCompilationOutput.dependencies, compilerDiagnostics = compilationResult.compilerDiagnostics,)
        } catch (e: Exception) {
            TranslationJSResult(exception = e.toExceptionDescriptor())
        }
    }

    private fun convertOutputToJsCode(wasmOutput: WasmTranslationSuccessfulOutput): String {
        return wasmOutput.jsCode
                    .replace(
                        "wasmInstance = (await WebAssembly.instantiateStreaming(fetch(new URL('./playground.wasm',import.meta.url).href), importObject, wasmOptions)).instance;",
                        "wasmInstance = await (async () => {\n" +
                                "  const wasmBase64 = '\${${Base64.encode(wasmOutput.wasm)}}';\n" +
                                "  const wasmBinary = Uint8Array.from(atob(wasmBase64), (c) => c.charCodeAt(0));\n" +
                                "  return (await WebAssembly.instantiate(wasmBinary, importObject)).instance;\n" +
                                "  })();"
                    )


    }

    fun doTranslateWithIr(
        files: List<ProjectFile>,
        arguments: List<String>,
        userCompilerArguments: JsCompilerArguments): CompilationResult<String> =
        usingTempDirectory { inputDir ->
            usingTempDirectory { outputDir ->
                val ioFiles = files.writeToIoFiles(inputDir)
                val k2JSCompiler = K2JSCompiler()
                val filePaths = ioFiles.map { it.toFile().canonicalPath }
                val klibPath = (outputDir / "klib").toFile().canonicalPath
                val additionalCompilerArgumentsForKLib =
                    compilerArgumentsUtil.convertCompilerArgumentsToCompilationString(
                        jsCompilerArguments,
                        compilerArgumentsUtil.PREDEFINED_JS_FIRST_PHASE_ARGUMENTS,
                        userCompilerArguments.firstPhase
                    ) + "-ir-output-dir=$klibPath"
                k2JSCompiler.tryCompilation(inputDir, ioFiles, filePaths + additionalCompilerArgumentsForKLib)
                    .flatMap {
                        val secondPhaseArguments =
                            compilerArgumentsUtil.convertCompilerArgumentsToCompilationString(
                                jsCompilerArguments,
                                compilerArgumentsUtil.PREDEFINED_JS_SECOND_PHASE_ARGUMENTS,
                                userCompilerArguments.secondPhase
                            ) + "-ir-output-dir=${(outputDir / "js").toFile().canonicalPath}" + "-Xinclude=$klibPath"
                        k2JSCompiler.tryCompilation(inputDir, ioFiles, secondPhaseArguments)
                    }
                    .map { (outputDir / "js" / "$JS_DEFAULT_MODULE_NAME.js").readText() }
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
        projectType: ProjectType,
        debugInfo: Boolean,
        userCompilerArguments: JsCompilerArguments
    ): CompilationResult<WasmTranslationSuccessfulOutput> =
        usingTempDirectory { inputDir ->
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

                val additionalCompilerArgumentsForKLib =
                    compilerArgumentsUtil.convertCompilerArgumentsToCompilationString(
                        defaultCompilerArgs,
                        firstPhasePredefinedArguments,
                        userCompilerArguments.firstPhase
                    ) + "-ir-output-dir=$klibPath"

                k2JSCompiler.tryCompilation(inputDir, ioFiles, filePaths + additionalCompilerArgumentsForKLib)
                    .flatMap {
                        val secondPhaseArguments = (compilerArgumentsUtil.convertCompilerArgumentsToCompilationString(
                            defaultCompilerArgs,
                            secondPhasePredefinedArguments,
                            userCompilerArguments.secondPhase
                        ) + "-ir-output-dir=${(outputDir / "wasm").toFile().canonicalPath}" + "-Xinclude=$klibPath").toMutableList()

                        if (debugInfo) secondPhaseArguments.add("-Xwasm-generate-wat")

                        k2JSCompiler.tryCompilation(inputDir, ioFiles, secondPhaseArguments)
                    }
                    .map {
                        val wasmOutputDir = outputDir / "wasm"
                        val dependencies = wasmOutputDir.listDirectoryEntries()
                            .filter { it.name != "$WASM_DEFAULT_MODULE_NAME.mjs" }
                            .filter { it.extension == "js" || it.extension == "mjs" }
                            .associate { it.name to it.readText() }

                        WasmTranslationSuccessfulOutput(
                            jsCode = (wasmOutputDir / "$WASM_DEFAULT_MODULE_NAME.mjs").readText(),
                            wasm = (wasmOutputDir / "$WASM_DEFAULT_MODULE_NAME.wasm").readBytes(),
                            dependencies = dependencies,
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
    val dependencies: Map<String, String>,
    val wasm: ByteArray
)
