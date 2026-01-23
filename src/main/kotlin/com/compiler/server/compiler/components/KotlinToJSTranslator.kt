package com.compiler.server.compiler.components

import com.compiler.server.common.components.*
import com.compiler.server.model.*
import com.compiler.server.utils.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.springframework.stereotype.Service
import kotlin.io.encoding.Base64
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.readText

private val JS_BUILTINS_ALIAS_NAME_REGEX =
    Regex("""import\s+\*\s+as\s+(\S+)\s+from\s+'\./$JS_DEFAULT_MODULE_NAME\.$JS_BUILTINS_POSTFIX\.mjs';""")

@Service
class KotlinToJSTranslator(
    private val compilerArgumentsUtil: CompilerArgumentsUtil,
    private val jsCompilerArguments: Set<ExtendedCompilerArgument>,
    private val wasmCompilerArguments: Set<ExtendedCompilerArgument>,
    private val composeWasmCompilerArguments: Set<ExtendedCompilerArgument>,
    private val dependenciesUtil: DependenciesUtil,
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
        translate: (
            List<ProjectFile>,
            ProjectType,
            Boolean,
            JsCompilerArguments
        ) -> CompilationResult<WasmTranslationSuccessfulOutput>
    ): TranslationResultWithJsCode {
        return try {
            val outputFileName = when (projectType) {
                ProjectType.WASM -> WASM_DEFAULT_MODULE_NAME
                ProjectType.COMPOSE_WASM -> "_${WASM_DEFAULT_MODULE_NAME}_"
                else -> throw IllegalStateException("Wasm should have wasm or compose-wasm project type")
            }

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
            TranslationWasmResult(
                jsCode = mergeWasmOutputIntoOneJs(wasmCompilationOutput, outputFileName).fixSkikoImports(),
                compilerDiagnostics = compilationResult.compilerDiagnostics
            )
        } catch (e: Exception) {
            TranslationWasmResult(exception = e.toExceptionDescriptor())
        }
    }

    fun doTranslateWithIr(
        files: List<ProjectFile>,
        arguments: List<String>,
        userCompilerArguments: JsCompilerArguments
    ): CompilationResult<String> =
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
                val (defaultCompilerArgs, firstPhasePredefinedArguments, secondPhasePredefinedArguments, outputFileName) = when (projectType) {
                    ProjectType.WASM -> WasmArguments(
                        wasmCompilerArguments,
                        compilerArgumentsUtil.PREDEFINED_WASM_FIRST_PHASE_ARGUMENTS,
                        compilerArgumentsUtil.PREDEFINED_WASM_SECOND_PHASE_ARGUMENTS,
                        WASM_DEFAULT_MODULE_NAME,
                    )

                    ProjectType.COMPOSE_WASM -> WasmArguments(
                        composeWasmCompilerArguments,
                        compilerArgumentsUtil.PREDEFINED_COMPOSE_WASM_FIRST_PHASE_ARGUMENTS,
                        compilerArgumentsUtil.PREDEFINED_COMPOSE_WASM_SECOND_PHASE_ARGUMENTS +
                                ("Xwasm-included-module-only" to true),
                        "_${WASM_DEFAULT_MODULE_NAME}_"
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

                        WasmTranslationSuccessfulOutput(
                            jsCode = (wasmOutputDir / "$outputFileName.mjs").readText(),
                            jsBuiltins = (wasmOutputDir / "$outputFileName.$JS_BUILTINS_POSTFIX.mjs")
                                .takeIf { it.exists() }
                                ?.readText(),
                            importObject = (wasmOutputDir / "$outputFileName.$IMPORT_OBJECT_POSTFIX.mjs").readText(),
                            wasm = (wasmOutputDir / "$outputFileName.wasm").readBytes(),
                        )
                    }
            }
        }

    private fun mergeWasmOutputIntoOneJs(
        wasmOutput: WasmTranslationSuccessfulOutput,
        outputFileName: String,
    ): String {
        val importObjectJsContent = wasmOutput.importObject

        val jsBuitinsAlias = JS_BUILTINS_ALIAS_NAME_REGEX.find(importObjectJsContent)?.groupValues?.get(1)

        val replacedImportObjectContent =
            wasmOutput.jsBuiltins?.toByteArray()?.let { byteContent ->
                importObjectJsContent.replace(
                    JS_BUILTINS_ALIAS_NAME_REGEX,
                    "const $jsBuitinsAlias = await import(`data:application/javascript;base64, ${Base64.encode(byteContent)}`)"
                )
            } ?: importObjectJsContent

        return wasmOutput.jsCode
            .replace(
                "import { importObject, setWasmExports } from './${outputFileName}.import-object.mjs'",
                "const { importObject, setWasmExports } = await import(`data:application/javascript;base64,${
                    Base64.encode(
                        replacedImportObjectContent.toByteArray()
                    )
                }`) "
            )
            .replace(
                "wasmInstance = (await WebAssembly.instantiateStreaming(fetch(new URL('./${outputFileName}.wasm',import.meta.url).href), importObject, wasmOptions)).instance;",
                "wasmInstance = await (async () => {\n" +
                        "  const wasmBase64 = await fetch(`data:application/wasm;base64,${Base64.encode(wasmOutput.wasm)}`); \n" +
                        "  const wasmBinary = new Uint8Array(await wasmBase64.arrayBuffer());\n" +
                        " if (typeof bufferedOutput !== 'undefined') {" +
                        "  importObject.js_code['kotlin.io.printImpl'] = (message) => bufferedOutput.buffer += message\n" +
                        "  importObject.js_code['kotlin.io.printlnImpl'] = (message) => {bufferedOutput.buffer += message;bufferedOutput.buffer += \"\\n\"}}\n" +
                        "  return (await WebAssembly.instantiate(wasmBinary, importObject)).instance;\n" +
                        "  })();"
            ) + "\n export const instantiate = () => Promise.resolve();"
    }

    private fun String.fixSkikoImports(): String = replace(
        "skiko.mjs",
        "skiko-${dependenciesUtil.dependenciesComposeWasm}.mjs"
    )
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
    val jsBuiltins: String?,
    val importObject: String,
    val wasm: ByteArray
)
