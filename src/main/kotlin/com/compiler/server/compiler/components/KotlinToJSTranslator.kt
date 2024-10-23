package com.compiler.server.compiler.components

import com.compiler.server.common.components.*
import com.compiler.server.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.psi.KtFile
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.time.measureTime

@Service
class KotlinToJSTranslator(
  private val kotlinEnvironment: KotlinEnvironment,
) {
  companion object {
    private const val JS_IR_CODE_BUFFER = "playground.output?.buffer_1;\n"

    private val JS_IR_OUTPUT_REWRITE = """
        if (typeof get_output !== "undefined") {
          get_output();
          output = new BufferedOutput();
          _.output = get_output();
        }
        """.trimIndent()

    const val BEFORE_MAIN_CALL_LINE = 4
  }

  fun translateJs(
    files: List<KtFile>,
    arguments: List<String>,
    translate: (List<KtFile>, List<String>) -> CompilationResult<String>
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
    files: List<KtFile>,
    debugInfo: Boolean,
    projectType: ProjectType,
    translate: (
      List<KtFile>,
      List<String>,
      List<String>,
      List<String>,
      File?,
      Boolean,
    ) -> CompilationResult<WasmTranslationSuccessfulOutput>
  ): TranslationResultWithJsCode {
    return try {
      val parameters: WasmParameters = when (projectType) {
        ProjectType.WASM -> WasmParameters(
          kotlinEnvironment.WASM_LIBRARIES,
          emptyList(),
          emptyList(),
          null
        )
        ProjectType.COMPOSE_WASM -> WasmParameters(
          kotlinEnvironment.COMPOSE_WASM_LIBRARIES,
          kotlinEnvironment.COMPOSE_WASM_COMPILER_PLUGINS,
          kotlinEnvironment.composeWasmCompilerPluginOptions,
          kotlinEnvironment.composeWasmCache,
        )
        else -> throw IllegalStateException("Wasm should have wasm or compose-wasm project type")
      }
      val compilationResult = translate(
        files,
        parameters.dependencies,
        parameters.plugins,
        parameters.pluginOptions,
        parameters.cacheDir,
        debugInfo,
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

  fun doTranslateWithIr(files: List<KtFile>, arguments: List<String>): CompilationResult<String> =
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
            k2JSCompiler.tryCompilation(inputDir, ioFiles, listOf(
              "-Xreport-all-warnings",
              "-Wextra",
              "-Xir-produce-js",
              "-Xir-dce",
              "-Xinclude=$klibPath",
              "-libraries=${kotlinEnvironment.JS_LIBRARIES.joinToString(PATH_SEPARATOR)}",
              "-ir-output-dir=${(outputDir / "js").toFile().canonicalPath}",
              "-ir-output-name=$moduleName",
            ))
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
    files: List<KtFile>,
    dependencies: List<String>,
    compilerPlugins: List<String>,
    compilerPluginOptions: List<String>,
    cacheDir: File?,
    debugInfo: Boolean,
  ): CompilationResult<WasmTranslationSuccessfulOutput> =
    usingTempDirectory { inputDir ->
      val moduleName = "playground"
      usingTempDirectory { outputDir ->
        val ioFiles = files.writeToIoFiles(inputDir)
        val k2JSCompiler = K2JSCompiler()
        val filePaths = ioFiles.map { it.toFile().canonicalPath }
        val klibPath = (outputDir / "klib").toFile().canonicalPath

        val compileAction: (icDir: Path?) -> CompilationResult<WasmTranslationSuccessfulOutput> = { icDir ->
          k2JSCompiler.tryCompilation(
            inputDir,
            ioFiles,
            compileWasmArgs(
              moduleName,
              filePaths,
              klibPath,
              compilerPlugins,
              compilerPluginOptions,
              dependencies
            )
          )
            .flatMap {
              k2JSCompiler.tryCompilation(
                inputDir, ioFiles,
                linkWasmArgs(
                  moduleName,
                  klibPath,
                  dependencies,
                  icDir,
                  outputDir,
                  debugInfo
                )
              )
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

        val a: CompilationResult<WasmTranslationSuccessfulOutput>

        val time = measureTime {
          a = cacheDir?.let { dir ->
            usingTempDirectory { tmpDir ->
              val cachesDir = tmpDir.resolve("caches").normalize()
              val originalCachesDirExists = dir.exists()
              if (originalCachesDirExists) {
                dir.copyRecursively(cachesDir.toFile())
              }
              val result = compileAction(cachesDir)
              if (!originalCachesDirExists) {
                cachesDir.toFile().copyRecursively(dir)
              }
              result
            }
          } ?: compileAction(null)
        }
        println("TIME: $time")
        a
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
