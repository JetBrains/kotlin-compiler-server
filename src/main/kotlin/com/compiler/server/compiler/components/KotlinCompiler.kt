package com.compiler.server.compiler.components

import com.compiler.server.executor.CommandLineArgument
import com.compiler.server.executor.JavaExecutor
import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.OutputDirectory
import com.compiler.server.model.bean.LibrariesFile
import com.compiler.server.model.toExceptionDescriptor
import component.KotlinEnvironment
import executors.JUnitExecutors
import executors.JavaRunnerExecutor
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassReader.*
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

@Component
class KotlinCompiler(
  private val kotlinEnvironment: KotlinEnvironment,
  private val javaExecutor: JavaExecutor,
  private val librariesFile: LibrariesFile,
  @Value("\${policy.file}") private val policyFileName: String
) {
  private val policyFile = File(policyFileName)

  data class JvmClasses(
    val files: Map<String, ByteArray> = emptyMap(),
    val mainClasses: Set<String> = emptySet()
  )

  fun run(files: List<KtFile>, args: String): ExecutionResult {
    return execute(files) { output, compiled ->
      val mainClass = JavaRunnerExecutor::class.java.name
      val compiledMainClass = when (compiled.mainClasses.size) {
        0 -> return@execute ExecutionResult(
          exception = IllegalArgumentException("No main method found in project").toExceptionDescriptor()
        )

        1 -> compiled.mainClasses.single()
        else -> return@execute ExecutionResult(
          exception = IllegalArgumentException(
            "Multiple classes in project contain main methods found: ${compiled.mainClasses.joinToString()}"
          ).toExceptionDescriptor()
        )
      }
      val arguments = listOfNotNull(compiledMainClass) + args.split(" ")
      javaExecutor.execute(argsFrom(mainClass, output, arguments))
        .asExecutionResult()
    }
  }

  fun test(files: List<KtFile>): ExecutionResult {
    return execute(files) { output, _ ->
      val mainClass = JUnitExecutors::class.java.name
      javaExecutor.execute(argsFrom(mainClass, output, listOf(output.path.toString())))
        .asJUnitExecutionResult()
    }
  }

  @OptIn(ExperimentalPathApi::class)
  fun compile(files: List<KtFile>): CompilationResult<JvmClasses> = usingTempDirectory { inputDir ->
    val ioFiles = files.writeToIoFiles(inputDir)
    usingTempDirectory { outputDir ->
      val arguments = ioFiles.map { it.absolutePathString() } + KotlinEnvironment.additionalCompilerArguments + listOf(
        "-cp", kotlinEnvironment.classpath.joinToString(PATH_SEPARATOR) { it.absolutePath },
        "-module-name", "web-module",
        "-no-stdlib", "-no-reflect",
        "-progressive",
        "-d", outputDir.absolutePathString()
      )
      K2JVMCompiler().tryCompilation(inputDir, ioFiles, arguments) {
        val outputFiles = buildMap {
          outputDir.visitFileTree {
            onVisitFile { file, _ ->
              put(file.relativeTo(outputDir).pathString, file.readBytes())
              FileVisitResult.CONTINUE
            }
          }
        }
        val mainClasses = findMainClasses(outputFiles)
        JvmClasses(
            files = outputFiles,
            mainClasses = mainClasses,
        )
      }
    }
  }

  private fun findMainClasses(outputFiles: Map<String, ByteArray>): Set<String> =
    outputFiles.mapNotNull { (name, bytes) ->
      if (!name.endsWith(".class")) return@mapNotNull null
      val reader = ClassReader(bytes)
      var hasMain = false
      val visitor = object : ClassVisitor(ASM9) {
        override fun visitMethod(
          access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?
        ): MethodVisitor? {
          if (name == "main" && descriptor == "([Ljava/lang/String;)V" && (access and ACC_PUBLIC != 0) && (access and ACC_STATIC != 0)) {
            hasMain = true
          }
          return null
        }
      }
      reader.accept(visitor, SKIP_CODE or SKIP_DEBUG or SKIP_FRAMES)
      if (hasMain) name.removeSuffix(".class").replace(File.separatorChar, '.') else null
    }.toSet()

  private fun execute(
    files: List<KtFile>,
    block: (output: OutputDirectory, compilation: JvmClasses) -> ExecutionResult
  ): ExecutionResult = try {
    when (val compilationResult = compile(files)) {
      is Compiled<JvmClasses> -> {
        usingTempDirectory { outputDir ->
          val output = write(compilationResult.result, outputDir)
          block(output, compilationResult.result).also {
            it.addWarnings(compilationResult.compilerDiagnostics)
          }
        }
      }

      is NotCompiled -> ExecutionResult(compilerDiagnostics = compilationResult.compilerDiagnostics)
    }
  } catch (e: Exception) {
    ExecutionResult(exception = e.toExceptionDescriptor())
  }

  private fun write(classes: JvmClasses, outputDir: Path): OutputDirectory {
    val libDir = librariesFile.jvm.absolutePath
    val policy = policyFile.readText()
      .replace("%%GENERATED%%", outputDir.toString().replace('\\', '/'))
      .replace("%%LIB_DIR%%", libDir.replace('\\', '/'))
    (outputDir / policyFile.name).apply { parent.toFile().mkdirs() }.toFile().writeText(policy)
    return OutputDirectory(outputDir, classes.files.map { (name, bytes) ->
      (outputDir / name).let { path ->
        path.parent.toFile().mkdirs()
        Files.write(path, bytes)
      }
    })
  }

  private fun argsFrom(
    mainClass: String?,
    outputDirectory: OutputDirectory,
    args: List<String>
  ): List<String> {
    val classPaths =
      (kotlinEnvironment.classpath.map { it.absolutePath } + outputDirectory.path.toAbsolutePath().toString())
        .joinToString(PATH_SEPARATOR)
    val policy = (outputDirectory.path / policyFile.name).toAbsolutePath()
    return CommandLineArgument(
      classPaths = classPaths,
      mainClass = mainClass,
      policy = policy,
      memoryLimit = 32,
      arguments = args
    ).toList()
  }
}
