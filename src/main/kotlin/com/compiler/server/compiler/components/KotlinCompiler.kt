package com.compiler.server.compiler.components

import com.compiler.server.executor.CommandLineArgument
import com.compiler.server.executor.JavaExecutor
import com.compiler.server.model.JvmExecutionResult
import com.compiler.server.model.OutputDirectory
import com.compiler.server.model.ProjectFile
import com.compiler.server.model.ProjectSeveriry
import com.compiler.server.model.bean.LibrariesFile
import com.compiler.server.model.toExceptionDescriptor
import component.KotlinEnvironment
import executors.JUnitExecutors
import executors.JavaRunnerExecutor
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchain
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.psi.KtFile
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo
import kotlin.io.path.visitFileTree

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

    private fun ByteArray.asHumanReadable(): String {
        val classReader = ClassReader(this)
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        val traceClassVisitor = TraceClassVisitor(printWriter)

        classReader.accept(traceClassVisitor, 0)

        return stringWriter.toString()
    }

    private fun JvmExecutionResult.addByteCode(compiled: JvmClasses) {
        jvmByteCode = compiled.files
            .mapNotNull { (_, bytes) -> runCatching { bytes.asHumanReadable() }.getOrNull() }
            .takeUnless { it.isEmpty() }
            ?.joinToString("\n\n")
    }

    fun run(files: List<ProjectFile>, addByteCode: Boolean, args: String): JvmExecutionResult {
        return execute(files, addByteCode) { output, compiled ->
            val mainClass = JavaRunnerExecutor::class.java.name
            val compiledMainClass = when (compiled.mainClasses.size) {
                0 -> return@execute JvmExecutionResult(
                    exception = IllegalArgumentException("No main method found in project").toExceptionDescriptor()
                )

                1 -> compiled.mainClasses.single()
                else -> return@execute JvmExecutionResult(
                    exception = IllegalArgumentException(
                        "Multiple classes in project contain main methods found: ${
                            compiled.mainClasses.sorted().joinToString()
                        }"
                    ).toExceptionDescriptor()
                )
            }
            val arguments = listOfNotNull(compiledMainClass) + args.split(" ")
            javaExecutor.execute(argsFrom(mainClass, output, arguments))
                .asExecutionResult()
        }
    }

    fun test(files: List<ProjectFile>, addByteCode: Boolean): JvmExecutionResult {
        return execute(files, addByteCode) { output, _ ->
            val mainClass = JUnitExecutors::class.java.name
            javaExecutor.execute(argsFrom(mainClass, output, listOf(output.path.toString())))
                .asJUnitExecutionResult()
        }
    }

  @OptIn(ExperimentalPathApi::class, ExperimentalBuildToolsApi::class)
  private fun compileWithBuildToolsApi(inputDir: Path, outputDir: Path, cp: String): CompilationResult<JvmClasses>? {
    try {
      val sources = inputDir.listDirectoryEntries()
      val toolchain = KotlinToolchain.loadImplementation(ClassLoader.getSystemClassLoader())
      val operation = toolchain.jvm.createJvmCompilationOperation(sources, outputDir)
      operation.compilerArguments[JvmCompilerArguments.JvmCompilerArgument<String?>("CLASSPATH")] = cp
      operation.compilerArguments[JvmCompilerArguments.JvmCompilerArgument<String?>("MODULE_NAME")] = "web-module"
      operation.compilerArguments[JvmCompilerArguments.JvmCompilerArgument<Boolean>("NO_STDLIB")] = true

    val session = toolchain.createBuildSession()

        try {
          val result = session.executeOperation(operation, toolchain.createInProcessExecutionPolicy())

          // Process output files
          val outputFiles = buildMap {
            outputDir.visitFileTree {
              onVisitFile { file, _ ->
                put(file.relativeTo(outputDir).pathString, file.readBytes())
                FileVisitResult.CONTINUE
              }
            }
          }

//          val mainClasses = findMainClasses(outputFiles)

          return if (result == org.jetbrains.kotlin.buildtools.api.CompilationResult.COMPILATION_SUCCESS) {
            Compiled(
              compilerDiagnostics = com.compiler.server.model.CompilerDiagnostics(emptyMap()),
              result = JvmClasses(
                files = outputFiles,
//                mainClasses = mainClasses,
              )
            )
          } else {
            NotCompiled(com.compiler.server.model.CompilerDiagnostics(emptyMap()))
          }
        }
        finally {
          /* TODO: Deal with NoSuchMethodError
          Possible reasons:
          - something is wrong in kotlin-build-tools-api/impl
          - there is a conflict between compiler-kotlin (which is often used in this project) and compiler-kotlin-embeddable (which should be used by impl)
           */
//          try{
            session.close()
//          }catch (_: NoSuchMethodError){}
        }
    } catch (e: Exception) {
      // Log the exception for debugging
      println("Error using kotlin-build-tools-api: ${e.message}")
      e.printStackTrace()

      // Return null to indicate that we should fall back to the old approach
      return null
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
        "-d", outputDir.absolutePathString(),
      ) + kotlinEnvironment.compilerPlugins.map { plugin -> "-Xplugin=${plugin.absolutePath}" }

      // Try the new approach first, fall back to the old one if it fails
      val classpath = kotlinEnvironment.classpath.joinToString(PATH_SEPARATOR) { it.absolutePath }
      val newApiResult = compileWithBuildToolsApi(inputDir, outputDir, classpath)

      // If the new approach succeeded, return its result
      if (newApiResult != null) {
        println("Successfully compiled with kotlin-build-tools-api")
        return@usingTempDirectory newApiResult
      }

      // Fall back to the old approach
      println("Falling back to K2JVMCompiler for compilation")
      org.jetbrains.kotlin.cli.jvm.K2JVMCompiler().tryCompilation(inputDir, ioFiles, arguments) {
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
      )
    }
  }
    @OptIn(ExperimentalPathApi::class)
    fun compile(files: List<ProjectFile>): CompilationResult<JvmClasses> = usingTempDirectory { inputDir ->
        val ioFiles = files.writeToIoFiles(inputDir)
        usingTempDirectory { outputDir ->
            val arguments =
                ioFiles.map { it.absolutePathString() } + KotlinEnvironment.additionalCompilerArguments + listOf(
                    "-cp", kotlinEnvironment.classpath.joinToString(PATH_SEPARATOR) { it.absolutePath },
                    "-module-name", "web-module",
                    "-no-stdlib", "-no-reflect",
                    "-progressive",
                    "-d", outputDir.absolutePathString(),
                ) + kotlinEnvironment.compilerPlugins.map { plugin -> "-Xplugin=${plugin.absolutePath}" }
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
        files: List<ProjectFile>,
        addByteCode: Boolean,
        block: (output: OutputDirectory, compilation: JvmClasses) -> JvmExecutionResult
    ): JvmExecutionResult = try {
        when (val compilationResult = compile(files)) {
            is Compiled<JvmClasses> -> {
                usingTempDirectory { outputDir ->
                    val output = write(compilationResult.result, outputDir)
                    block(output, compilationResult.result).also {
                        it.addWarnings(compilationResult.compilerDiagnostics)
                        if (addByteCode) {
                            it.addByteCode(compilationResult.result)
                        }
                    }
                }
            }

            is NotCompiled -> JvmExecutionResult(compilerDiagnostics = compilationResult.compilerDiagnostics)
        }
    } catch (e: Exception) {
        JvmExecutionResult(exception = e.toExceptionDescriptor())
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
