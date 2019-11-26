package com.compiler.server.compiler.components

import com.compiler.server.executor.CommandLineArgument
import com.compiler.server.executor.JUnitExecutor
import com.compiler.server.executor.JavaExecutor
import com.compiler.server.model.ExceptionDescriptor
import com.compiler.server.model.JavaExecutionResult
import com.compiler.server.model.OutputDirectory
import executors.JUnitExecutors
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.junit.Test
import org.springframework.stereotype.Component
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@Component
class KotlinCompiler(
  private val errorAnalyzer: ErrorAnalyzer,
  private val kotlinEnvironment: KotlinEnvironment,
  private val javaExecutor: JavaExecutor,
  private val jUnitExecutor: JUnitExecutor
) {

  class Compiled(val files: Map<String, ByteArray> = emptyMap(), val mainClass: String? = null)

  fun run(files: List<KtFile>, args: String): JavaExecutionResult {
    return execute(files) { output, compiled ->
      val arguments = args.split(" ")
      javaExecutor.execute(argsFrom(compiled.mainClass, output, arguments))
    }
  }

  fun test(files: List<KtFile>): JavaExecutionResult {
    return execute(files) { output, _ ->
      val testClasses = loadTestClasses(output)
      val mainClass = JUnitExecutors::class.java.name
      jUnitExecutor.execute(argsFrom(
        mainClass = mainClass,
        outputDirectory = output,
        args = listOf(output.path.toString())
      ))
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

  private fun execute(
    files: List<KtFile>,
    block: (output: OutputDirectory, compilation: Compiled) -> JavaExecutionResult
  ): JavaExecutionResult {
    val errors = errorAnalyzer.errorsFrom(files)
    return if (errorAnalyzer.isOnlyWarnings(errors)) {
      val compilation = compile(files)
      if (compilation.files.isEmpty())
        return JavaExecutionResult(
          text = "",
          exception = ExceptionDescriptor("Something went wrong", "Exception")
        )
      val output = write(compilation)
      block(output, compilation).also { output.path.toAbsolutePath().toFile().deleteRecursively() }
    }
    else JavaExecutionResult(errors = errors)
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
    mainClass: String?,
    outputDirectory: OutputDirectory,
    args: List<String>
  ): List<String> {
    val classPaths = (kotlinEnvironment.classpath.map { it.absolutePath } + outputDirectory.path.toAbsolutePath().toString())
      .joinToString(":")
    val policy = outputDirectory.path.resolve("executor.policy").toAbsolutePath()
    return CommandLineArgument(
      classPaths = classPaths,
      mainClass = mainClass,
      policy = policy,
      memoryLimit = 32,
      arguments = args
    ).toList()
  }


  private fun mainClassFrom(bindingContext: BindingContext, files: List<KtFile>): String? {
    val mainFunctionDetector = MainFunctionDetector(bindingContext, LanguageVersionSettingsImpl.DEFAULT)
    return files.find { mainFunctionDetector.hasMain(it.declarations) }?.let {
      PackagePartClassUtils.getPackagePartFqName(it.packageFqName, it.name).asString()
    }
  }

  private fun loadTestClasses(output: OutputDirectory): List<String> {
    val urls = listOf(output.path.toUri().toURL()).toTypedArray()
    val loader = URLClassLoader(urls)
    val names = output.path.toFile().listFiles().orEmpty()
      .filter { it.name.endsWith(".class") }
      .map { it.name.removeSuffix(".class") }
    return names.mapNotNull {
      try {
        loader.loadClass(it)
      }
      catch (_: Exception) {
        null
      }
    }.filter { it -> it.methods.any { it.isAnnotationPresent(Test::class.java) } }.map { it.name }
  }
}