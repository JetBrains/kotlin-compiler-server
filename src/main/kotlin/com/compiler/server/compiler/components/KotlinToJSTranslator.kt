package com.compiler.server.compiler.components

import com.compiler.server.model.*
import component.KotlinEnvironment
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.wasm.compileToLoweredIr
import org.jetbrains.kotlin.backend.wasm.compileWasm
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.backend.wasm.wasmPhases
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.K2JSTranslator
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationResult
import org.jetbrains.kotlin.js.facade.exceptions.TranslationException
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.springframework.stereotype.Service

@Service
class KotlinToJSTranslator(
  private val kotlinEnvironment: KotlinEnvironment,
  private val errorAnalyzer: ErrorAnalyzer
) {
  companion object {
    private const val JS_CODE_FLUSH = "kotlin.kotlin.io.output.flush();\n"
    private const val JS_CODE_BUFFER = "\nkotlin.kotlin.io.output.buffer;\n"

    private const val JS_IR_CODE_BUFFER = "moduleId.output?.buffer_1;\n"

    private val JS_IR_OUTPUT_REWRITE = """
        if (typeof get_output !== "undefined") {
          get_output();
          output = new BufferedOutput();
          _.output = get_output();
        }
        """.trimIndent()

    const val BEFORE_MAIN_CALL_LINE = 4
  }

  fun translate(
    files: List<KtFile>,
    arguments: List<String>,
    coreEnvironment: KotlinCoreEnvironment,
    translate: (List<KtFile>, List<String>, KotlinCoreEnvironment) -> TranslationResultWithJsCode
  ): TranslationResultWithJsCode {
    val (errors, _) = errorAnalyzer.errorsFrom(files, coreEnvironment, isJs = true)
    return try {
      if (errorAnalyzer.isOnlyWarnings(errors)) {
        translate(files, arguments, coreEnvironment).also {
          it.addWarnings(errors)
        }
      } else {
        TranslationJSResult(errors = errors)
      }
    } catch (e: Exception) {
      TranslationJSResult(exception = e.toExceptionDescriptor())
    }
  }

  @Throws(TranslationException::class)
  fun doTranslate(
    files: List<KtFile>,
    arguments: List<String>,
    coreEnvironment: KotlinCoreEnvironment
  ): TranslationJSResult {
    val currentProject = coreEnvironment.project
    val configuration = JsConfig(
      currentProject,
      kotlinEnvironment.jsConfiguration,
      CompilerEnvironment,
      kotlinEnvironment.JS_METADATA_CACHE,
      kotlinEnvironment.JS_LIBRARIES.toSet()
    )
    val reporter = object : JsConfig.Reporter() {
      override fun error(message: String) {}
      override fun warning(message: String) {}
    }
    val translator = K2JSTranslator(configuration)
    val result = translator.translate(
      reporter = reporter,
      files = files,
      mainCallParameters = MainCallParameters.mainWithArguments(arguments)
    )
    return if (result is TranslationResult.Success) {
      TranslationJSResult(JS_CODE_FLUSH + result.getCode() + JS_CODE_BUFFER)
    } else {
      val errors = HashMap<String, List<ErrorDescriptor>>()
      for (psiFile in files) {
        errors[psiFile.name] = ArrayList()
      }
      errorAnalyzer.errorsFrom(result.diagnostics.all(), errors, isJs = true)
      TranslationJSResult(errors = errors)
    }
  }

  fun doTranslateWithIr(
    files: List<KtFile>,
    arguments: List<String>,
    coreEnvironment: KotlinCoreEnvironment
  ): TranslationJSResult {
    val currentProject = coreEnvironment.project

    val sourceModule = prepareAnalyzedSourceModule(
      currentProject,
      files,
      kotlinEnvironment.jsConfiguration,
      kotlinEnvironment.JS_LIBRARIES,
      friendDependencies = emptyList(),
      analyzer = AnalyzerWithCompilerReport(kotlinEnvironment.jsConfiguration),
    )
    val ir = compile(
      sourceModule,
      kotlinEnvironment.jsIrPhaseConfig,
      irFactory = IrFactoryImplForJsIC(WholeWorldStageController())
    )
    val transformer = IrModuleToJsTransformer(
      ir.context,
      arguments
    )

    val compiledModule: CompilerResult = transformer.generateModule(
      modules = ir.allModules,
      modes = setOf(TranslationMode.FULL_PROD),
      relativeRequirePath = false
    )

    val jsCode = getJsCodeFromModule(compiledModule)

    val listLines = jsCode
      .lineSequence()
      .toMutableList()

    listLines.add(listLines.size - BEFORE_MAIN_CALL_LINE, JS_IR_OUTPUT_REWRITE)
    listLines.add(listLines.size - 1, JS_IR_CODE_BUFFER)

    return TranslationJSResult(listLines.joinToString("\n"))
  }

  fun doTranslateWithWasm(
    files: List<KtFile>,
    arguments: List<String>,
    coreEnvironment: KotlinCoreEnvironment
  ): TranslationWasmResult {
    val currentProject = coreEnvironment.project

    val sourceModule = prepareAnalyzedSourceModule(
      currentProject,
      files,
      kotlinEnvironment.wasmConfiguration,
      kotlinEnvironment.WASM_LIBRARIES,
      friendDependencies = emptyList(),
      analyzer = AnalyzerWithCompilerReport(kotlinEnvironment.wasmConfiguration),
    )

    val (allModules, backendContext) = compileToLoweredIr(
      depsDescriptors = sourceModule,
      phaseConfig = PhaseConfig(wasmPhases),
      irFactory = IrFactoryImpl,
      exportedDeclarations = setOf(FqName("main")),
      propertyLazyInitialization = true,
    )
    eliminateDeadDeclarations(allModules, backendContext)

    val res = compileWasm(
      allModules = allModules,
      backendContext = backendContext,
      baseFileName = "moduleId",
      emitNameSection = false,
      allowIncompleteImplementations = true,
      generateWat = false,
      generateSourceMaps = false
    )

    return TranslationWasmResult(res.jsUninstantiatedWrapper, res.jsWrapper, res.wasm)
  }

  private fun getJsCodeFromModule(compiledModule: CompilerResult): String {
    val jsCodeObject = compiledModule.outputs.values.single()

    val jsCodeClass = jsCodeObject.javaClass
    val jsCode = jsCodeClass.getDeclaredField("rawJsCode").let {
      it.isAccessible = true
      it.get(jsCodeObject) as String
    }
    return jsCode
  }
}