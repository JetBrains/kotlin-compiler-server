package com.compiler.server.compiler.components

import com.compiler.server.model.BundleType
import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ExceptionDescriptor
import com.compiler.server.model.TranslationJSResult
import com.compiler.server.model.bean.LibrariesFile
import com.compiler.server.model.toExceptionDescriptor
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.K2JSTranslator
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationResult
import org.jetbrains.kotlin.js.facade.exceptions.TranslationException
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class KotlinToJSTranslator(
  private val kotlinEnvironment: KotlinEnvironment,
  private val errorAnalyzer: ErrorAnalyzer,
  private val librariesFile: LibrariesFile
) {

  @Value("classpath:package.json")
  lateinit var packageJson: Resource

  @Value("classpath:webpack.config.plain.js")
  lateinit var webpackConfigPlain: Resource
  @Value("classpath:webpack.config.minimized.js")
  lateinit var webpackConfigMinimized: Resource

  private val JS_CODE_FLUSH = "kotlin.kotlin.io.output.flush();\n"
  private val JS_CODE_BUFFER = "\nkotlin.kotlin.io.output.buffer;\n"

  fun translate(files: List<KtFile>, arguments: List<String>, bundleType: BundleType): TranslationJSResult {
    val errors = errorAnalyzer.errorsFrom(files, isJs = true)
    return try {
      if (errorAnalyzer.isOnlyWarnings(errors)) {
        val result = doTranslate(files, arguments, bundleType).also {
          it.addWarnings(errors)
        }
        if (bundleType == BundleType.NONE) {
          result
        } else {
          webpackBundle(result, bundleType)
        }
      }
      else {
        TranslationJSResult(errors = errors)
      }
    }
    catch (e: Exception) {
      TranslationJSResult(exception = e.toExceptionDescriptor())
    }
  }

  @Throws(TranslationException::class)
  private fun doTranslate(
    files: List<KtFile>,
    arguments: List<String>,
    bundleType: BundleType
  ): TranslationJSResult {
    val currentProject = kotlinEnvironment.coreEnvironment.project
    val jsEnvironment = if (bundleType == BundleType.NONE) {
      kotlinEnvironment.jsEnvironment
    } else {
      kotlinEnvironment.jsEnvironment.copy().apply {
        put(JSConfigurationKeys.MODULE_KIND, ModuleKind.UMD)
      }
    }
    val configuration = JsConfig(currentProject, jsEnvironment)
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
      if (bundleType == BundleType.NONE) {
        TranslationJSResult(JS_CODE_FLUSH + result.getCode() + JS_CODE_BUFFER)
      } else {
        TranslationJSResult(result.getCode())
      }
    }
    else {
      val errors = HashMap<String, List<ErrorDescriptor>>()
      for (psiFile in files) {
        errors[psiFile.name] = ArrayList()
      }
      errorAnalyzer.errorsFrom(result.diagnostics.all(), errors)
      TranslationJSResult(errors = errors)
    }
  }

  private fun webpackBundle(jsResult: TranslationJSResult, bundleType: BundleType): TranslationJSResult {
    val jsCode = jsResult.jsCode
    return if (jsCode != null) {
      val tempDir = Files.createTempDirectory("kotlin-compiler-server").toFile()
      try {
        File(tempDir, "main.js").writeText(jsCode)
        File(tempDir, "package.json").writeText(packageJson.asString())
        val webpackConfig = if (bundleType == BundleType.PLAIN)
          webpackConfigPlain
        else
          webpackConfigMinimized
        File(tempDir, "webpack.config.js").writeText(
          webpackConfig.asString()
            .replace("###EXECUTOR_DIR###", librariesFile.js.absolutePath)
            .replace("###TEMP_DIR###", tempDir.absolutePath)
        )
        runCommand("yarn install", tempDir)
        runCommand("node_modules/.bin/webpack", tempDir)
        val resultFile = File(tempDir, "main.bundle.js")
        if (resultFile.exists()) {
          jsResult.copy(jsCode = resultFile.readText())
        } else {
          jsResult.copy(exception = ExceptionDescriptor("Webpack processing failed"))
        }
      } catch (e: IOException) {
        jsResult.copy(exception = e.toExceptionDescriptor())
      } finally {
        tempDir.deleteRecursively()
      }
    } else {
      jsResult.copy(exception = ExceptionDescriptor("Translated code is empty"))
    }
  }

  private fun runCommand(
    command: String,
    workingDir: File = File("."),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS
  ): String {
    return ProcessBuilder(command.split("\\s".toRegex()))
      .directory(workingDir)
      .redirectOutput(ProcessBuilder.Redirect.PIPE)
      .redirectError(ProcessBuilder.Redirect.PIPE)
      .start().apply { waitFor(timeoutAmount, timeoutUnit) }
      .inputStream.bufferedReader().readText()
  }

  private fun Resource.asString(): String {
    InputStreamReader(this.inputStream, Charsets.UTF_8).use { reader ->
      return FileCopyUtils.copyToString(reader)
    }
  }
}
