package com.compiler.server.compiler.components

import com.compiler.server.executor.CommandLineArgument
import com.compiler.server.executor.ExecutorMessages
import com.compiler.server.executor.JavaExecutor
import com.compiler.server.model.*
import com.compiler.server.model.bean.LibrariesFile
import component.KotlinEnvironment
import executors.JUnitExecutors
import executors.JavaRunnerExecutor
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.findMainClass
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.DefaultCodegenFactory
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.psi.KtFile
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@Component
class KotlinCompiler(
  private val errorAnalyzer: ErrorAnalyzer,
  private val kotlinEnvironment: KotlinEnvironment,
  private val javaExecutor: JavaExecutor,
  private val librariesFile: LibrariesFile,
  @Value("\${policy.file}") private val policyFileName: String
) {
  private val policyFile = File(policyFileName)

  companion object {
    private val PATH_SEPARATOR = System.getProperty("path.separator") ?: ":"
  }

  class Compiled(val files: Map<String, ByteArray> = emptyMap(), val mainClass: String? = null)

  fun run(files: List<KtFile>, coreEnvironment: KotlinCoreEnvironment, args: String): ExecutionResult {
    return execute(files, coreEnvironment) { output, compiled ->
      val mainClass = JavaRunnerExecutor::class.java.name
      val arguments = listOfNotNull(compiled.mainClass) + args.split(" ")
      javaExecutor.execute(argsFrom(mainClass, output, arguments))
        .asExecutionResult()
    }
  }

  fun test(files: List<KtFile>, coreEnvironment: KotlinCoreEnvironment): ExecutionResult {
    return execute(files, coreEnvironment) { output, _ ->
      val mainClass = JUnitExecutors::class.java.name
      javaExecutor.execute(argsFrom(mainClass, output, listOf(output.path.toString())))
        .asJUnitExecutionResult()
    }
  }

  private fun compile(files: List<KtFile>, analysis: Analysis, coreEnvironment: KotlinCoreEnvironment): Compiled {
    val generationState = generationStateFor(files, analysis, coreEnvironment)
    KotlinCodegenFacade.compileCorrectFiles(generationState)
    return Compiled(
      files = generationState.factory.asList().associate { it.relativePath to it.asByteArray() },
      mainClass = findMainClass(
        generationState.bindingContext,
        LanguageVersionSettingsImpl.DEFAULT,
        files
      )?.asString()
    )
  }

  private fun execute(
    files: List<KtFile>,
    coreEnvironment: KotlinCoreEnvironment,
    block: (output: OutputDirectory, compilation: Compiled) -> ExecutionResult
  ): ExecutionResult {
    return try {
      val (errors, analysis) = errorAnalyzer.errorsFrom(
        files = files,
        coreEnvironment = coreEnvironment,
        isJs = false
      )
      return if (errorAnalyzer.isOnlyWarnings(errors)) {
        val compilation = compile(files, analysis, coreEnvironment)
        if (compilation.files.isEmpty())
          return ProgramOutput(restriction = ExecutorMessages.NO_COMPILERS_FILE_FOUND).asExecutionResult()
        val output = write(compilation)
        try {
          block(output, compilation).also {
            it.addWarnings(errors)
          }
        } finally {
          output.path.toAbsolutePath().toFile().deleteRecursively()
        }
      } else ExecutionResult(errors)
    } catch (e: Exception) {
      ExecutionResult(exception = e.toExceptionDescriptor())
    }
  }

  private fun write(compiled: Compiled): OutputDirectory {
    val dir = System.getProperty("java.io.tmpdir")
    val libDir = librariesFile.jvm.absolutePath
    val sessionId = UUID.randomUUID().toString().replace("-", "")
    val outputDir = Paths.get(dir, sessionId)
    val policy = policyFile.readText()
      .replace("%%GENERATED%%", outputDir.toString().replace('\\', '/'))
      .replace("%%LIB_DIR%%", libDir.replace('\\', '/'))
    outputDir.resolve(policyFile.name).apply { parent.toFile().mkdirs() }.toFile().writeText(policy)
    return OutputDirectory(outputDir, compiled.files.map { (name, bytes) ->
      outputDir.resolve(name).let { path ->
        path.parent.toFile().mkdirs()
        Files.write(path, bytes)
      }
    })
  }

  private fun generationStateFor(
    files: List<KtFile>,
    analysis: Analysis,
    coreEnvironment: KotlinCoreEnvironment
  ): GenerationState {
    val codegenFactory = getCodegenFactory(coreEnvironment)
    return GenerationState.Builder(
      project = files.first().project,
      builderFactory = ClassBuilderFactories.BINARIES,
      module = analysis.analysisResult.moduleDescriptor,
      bindingContext = analysis.analysisResult.bindingContext,
      files = files,
      configuration = coreEnvironment.configuration
    ).codegenFactory(codegenFactory).build()
  }

  private fun getCodegenFactory(coreEnvironment: KotlinCoreEnvironment): CodegenFactory {
    return if (coreEnvironment.configuration.getBoolean(JVMConfigurationKeys.IR))
      JvmIrCodegenFactory(
        coreEnvironment.configuration,
        coreEnvironment.configuration.get(CLIConfigurationKeys.PHASE_CONFIG) ?: PhaseConfig(jvmPhases)
      ) else
      DefaultCodegenFactory
  }

  private fun argsFrom(
    mainClass: String?,
    outputDirectory: OutputDirectory,
    args: List<String>
  ): List<String> {
    val classPaths =
      (kotlinEnvironment.classpath.map { it.absolutePath } + outputDirectory.path.toAbsolutePath().toString())
        .joinToString(PATH_SEPARATOR)
    val policy = outputDirectory.path.resolve(policyFile.name).toAbsolutePath()
    return CommandLineArgument(
      classPaths = classPaths,
      mainClass = mainClass,
      policy = policy,
      memoryLimit = 32,
      arguments = args
    ).toList()
  }

}