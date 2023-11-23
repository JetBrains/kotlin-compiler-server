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
import org.jetbrains.kotlin.ir.backend.js.dce.DceDumpNameCache
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
    projectType: ProjectType,
    translate: (ModulesStructure, List<String>, KotlinCoreEnvironment) -> TranslationResultWithJsCode
  ): TranslationResultWithJsCode {
    val (errors, analysis) = errorAnalyzer.errorsFrom(files, coreEnvironment, projectType = projectType)
    return try {
      if (errorAnalyzer.isOnlyWarnings(errors)) {
        translate((analysis as AnalysisJs).sourceModule, arguments, coreEnvironment).also {
          it.addWarnings(errors)
        }
      } else {
        TranslationJSResult(errors = errors)
      }
    } catch (e: Exception) {
      TranslationJSResult(exception = e.toExceptionDescriptor())
    }
  }

  fun doTranslateWithIr(
    sourceModule: ModulesStructure,
    arguments: List<String>,
    coreEnvironment: KotlinCoreEnvironment
  ): TranslationJSResult {
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
    sourceModule: ModulesStructure,
    arguments: List<String>,
    coreEnvironment: KotlinCoreEnvironment
  ): TranslationWasmResult {
    val (allModules, backendContext) = compileToLoweredIr(
      depsDescriptors = sourceModule,
      phaseConfig = PhaseConfig(wasmPhases),
      irFactory = IrFactoryImpl,
      exportedDeclarations = setOf(FqName("main")),
      propertyLazyInitialization = true,
    )
    eliminateDeadDeclarations(allModules, backendContext, DceDumpNameCache())

    val res = compileWasm(
      allModules = allModules,
      backendContext = backendContext,
      baseFileName = "moduleId",
      emitNameSection = false,
      allowIncompleteImplementations = true,
      generateWat = true,
      generateSourceMaps = false
    )

    return TranslationWasmResult(res.jsUninstantiatedWrapper, res.jsWrapper, res.wasm, res.wat)
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