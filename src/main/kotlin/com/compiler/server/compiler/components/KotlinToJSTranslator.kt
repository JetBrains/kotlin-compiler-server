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
import com.compiler.server.utils.IMPORT_OBJECT_POSTFIX
import com.compiler.server.utils.JS_BUILTINS_POSTFIX
import com.compiler.server.utils.JS_DEFAULT_MODULE_NAME
import com.compiler.server.utils.WASM_DEFAULT_MODULE_NAME
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.springframework.stereotype.Service
import kotlin.io.encoding.Base64
import kotlin.io.path.div
import kotlin.io.path.readBytes
import kotlin.io.path.readText

private val JS_BUILTINS_ALIAS_NAME_REGEX =
    Regex("""import\s+\*\s+as\s+(\S+)\s+from\s+'\./$JS_DEFAULT_MODULE_NAME\.$JS_BUILTINS_POSTFIX\.mjs';""")

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
            TranslationWasmResult(
                jsCode = mergeWasmOutputIntoOneJs(wasmCompilationOutput),
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

                        WasmTranslationSuccessfulOutput(
                            jsCode = (wasmOutputDir / "$JS_DEFAULT_MODULE_NAME.mjs").readText(),
                            jsBuiltins = (wasmOutputDir / "$JS_DEFAULT_MODULE_NAME.$JS_BUILTINS_POSTFIX.mjs").readText(),
                            importObject = (wasmOutputDir / "$JS_DEFAULT_MODULE_NAME.$IMPORT_OBJECT_POSTFIX.mjs").readText(),
                            wasm = (wasmOutputDir / "$WASM_DEFAULT_MODULE_NAME.wasm").readBytes(),
                        )
                    }
            }
        }

    private fun mergeWasmOutputIntoOneJs(wasmOutput: WasmTranslationSuccessfulOutput): String {
        val importObjectJsContent = wasmOutput.importObject

        val jsBuitinsAlias = JS_BUILTINS_ALIAS_NAME_REGEX.find(importObjectJsContent)?.groupValues?.get(1)

        val replacedImportObjectContent = importObjectJsContent.replace(
            JS_BUILTINS_ALIAS_NAME_REGEX,
            "const $jsBuitinsAlias = await import(`data:application/javascript;base64, ${Base64.encode(wasmOutput.jsBuiltins.toByteArray())}`)"
        )

        return wasmOutput.jsCode
            .replace(
                "import { importObject, setWasmExports } from './playground.import-object.mjs'",
                "const { importObject, setWasmExports } = await import(`data:application/javascript;base64,${
                    Base64.encode(
                        replacedImportObjectContent.toByteArray()
                    )
                }`) "
            )
            .replace(
                "wasmInstance = (await WebAssembly.instantiateStreaming(fetch(new URL('./playground.wasm',import.meta.url).href), importObject, wasmOptions)).instance;",
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
    val jsBuiltins: String,
    val importObject: String,
    val wasm: ByteArray
)
