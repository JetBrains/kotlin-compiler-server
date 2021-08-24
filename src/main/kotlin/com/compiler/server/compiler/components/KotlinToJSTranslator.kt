package com.compiler.server.compiler.components

import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.TranslationJSResult
import com.compiler.server.model.toExceptionDescriptor
import component.KotlinEnvironment
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.compile
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.K2JSTranslator
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationResult
import org.jetbrains.kotlin.js.facade.exceptions.TranslationException
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.springframework.stereotype.Service
import java.util.*

@Service
class KotlinToJSTranslator(
  private val kotlinEnvironment: KotlinEnvironment,
  private val errorAnalyzer: ErrorAnalyzer
) {
  companion object {
    private const val JS_CODE_FLUSH = "kotlin.kotlin.io.output.flush();\n"
    private const val JS_CODE_BUFFER = "\nkotlin.kotlin.io.output.buffer;\n"

    private const val JS_IR_CODE_BUFFER = "moduleId.output._buffer;\n"

    const val BEFORE_MAIN_CALL_LINE = 4
  }

  fun translate(
    files: List<KtFile>,
    arguments: List<String>,
    coreEnvironment: KotlinCoreEnvironment,
    translate: (List<KtFile>, List<String>, KotlinCoreEnvironment) -> TranslationJSResult
  ): TranslationJSResult {
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
    val configuration = JsConfig(
      currentProject,
      kotlinEnvironment.jsConfiguration,
      CompilerEnvironment,
      kotlinEnvironment.JS_METADATA_CACHE,
      kotlinEnvironment.JS_LIBRARIES.toSet()
    )

    val result = compile(
      currentProject,
      MainModule.SourceFiles(files),
      AnalyzerWithCompilerReport(configuration.configuration),
      configuration.configuration,
      kotlinEnvironment.jsIrPhaseConfig,
      allDependencies = kotlinEnvironment.jsIrResolvedLibraries,
      friendDependencies = emptyList(),
      propertyLazyInitialization = false,
      mainArguments = arguments,
      irFactory = IrFactoryImpl
    )
    val jsCode = result.outputs!!.jsCode

    val listLines = jsCode
      .lineSequence()
      .toMutableList()

    listLines.add(listLines.size - BEFORE_MAIN_CALL_LINE, "if (kotlin.isRewrite) output = new BufferedOutput_0()")
    listLines.add(listLines.size - BEFORE_MAIN_CALL_LINE, "_.output = output")
    listLines.add(listLines.size - 1, JS_IR_CODE_BUFFER)

    return TranslationJSResult(listLines.joinToString("\n"))
  }
}