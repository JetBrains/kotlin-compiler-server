package com.compiler.server.compiler.components

import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.model.ExceptionDescriptor
import com.compiler.server.compiler.model.ExecutionResult
import com.compiler.server.compiler.model.JavaExecutionResult
import com.compiler.server.compiler.model.OutputDirectory
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@Component
class KotlinCompiler(
  private val errorAnalyzer: ErrorAnalyzer,
  private val kotlinEnvironment: KotlinEnvironment,
  private val javaExecutor: JavaExecutor
) {

  class Compiled(val files: Map<String, ByteArray> = emptyMap(), val mainClass: String? = null)

  fun run(files: List<KotlinFile>): ExecutionResult {
    val errors = errorAnalyzer.errorsFrom(files.map { it.kotlinFile })
    return if (errorAnalyzer.isOnlyWarnings(errors)) {
      val compilation = compile(files.map { it.kotlinFile })
      execute(compilation)
    }
    else {
      ExecutionResult(errors = errors)
    }
  }

  private fun compile(files: List<KtFile>): Compiled {
    val generationState = generationStateFor(files)
    KotlinCodegenFacade.compileCorrectFiles(generationState) { error, _ -> error.printStackTrace() }
    return Compiled(
      files = generationState.factory.asList().map { it.relativePath to it.asByteArray() }.toMap(),
      mainClass = mainClassFrom(generationState.bindingContext, files)
    )
  }

  private fun execute(compiled: Compiled): JavaExecutionResult {
    if (compiled.files.isEmpty())
      return JavaExecutionResult(
        text = "",
        exception = ExceptionDescriptor("Something went wrong", "Exception")
      )
    val output = write(compiled)
    return javaExecutor.execute(argsFrom(compiled.mainClass!!, output, kotlinEnvironment))
      .also { output.path.toAbsolutePath().toFile().deleteRecursively() }
  }

  private fun write(compiled: Compiled): OutputDirectory {
    val dir = System.getProperty("user.dir")
    val sessionId = UUID.randomUUID().toString().replace("-", "")
    val outputDir = Paths.get(dir, "generated", sessionId)
    val policy = File("executor.policy").readText()
      .replace("%%GENERATED%%", outputDir.toString())
      .replace("%%LIB_DIR%%", dir)
    outputDir.resolve("executor.policy").apply { parent.toFile().mkdirs() }.toFile().writeText(policy)
    return OutputDirectory(outputDir, compiled.files.map { (name, bytes) ->
      outputDir.resolve(name).let { path ->
        path.parent.toFile().mkdirs()
        Files.write(path, bytes)
      }
    })
  }

  private fun generationStateFor(files: List<KtFile>): GenerationState {
    val analysis = errorAnalyzer.analysisOf(files)
    return GenerationState.Builder(
      files.first().project,
      ClassBuilderFactories.BINARIES,
      analysis.analysisResult.moduleDescriptor,
      analysis.analysisResult.bindingContext,
      files,
      kotlinEnvironment.coreEnvironment.configuration
    ).build()
  }

  private fun argsFrom(
    mainClass: String,
    outputDirectory: OutputDirectory,
    environment: KotlinEnvironment
  ): List<String> {
    val classPaths = (environment.classpath.map { it.absolutePath } + outputDirectory.path.toAbsolutePath().toString())
      .joinToString(":")
    val policy = outputDirectory.path.resolve("executor.policy").toAbsolutePath()
    return JavaArgumentsBuilder(
      classPaths = classPaths,
      mainClass = mainClass,
      policy = policy,
      memoryLimit = 32
    ).toArguments()
  }


  private fun mainClassFrom(bindingContext: BindingContext, files: List<KtFile>): String? {
    val mainFunctionDetector = MainFunctionDetector(bindingContext, LanguageVersionSettingsImpl.DEFAULT)
    return files.find { mainFunctionDetector.hasMain(it.declarations) }?.let {
      PackagePartClassUtils.getPackagePartFqName(it.packageFqName, it.name).asString()
    }
  }
}