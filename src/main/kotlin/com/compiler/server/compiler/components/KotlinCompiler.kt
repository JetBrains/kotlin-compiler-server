package com.compiler.server.compiler.components

import com.compiler.server.executor.CommandLineArgument
import com.compiler.server.executor.JavaExecutor
import com.compiler.server.model.CompilerDiagnostics
import com.compiler.server.model.ExtendedCompilerArgument
import com.compiler.server.model.JvmExecutionResult
import com.compiler.server.model.OutputDirectory
import com.compiler.server.model.ProjectFile
import com.compiler.server.model.bean.LibrariesFile
import com.compiler.server.model.toExceptionDescriptor
import com.compiler.server.utils.CompilerArgumentsUtil
import component.KotlinEnvironment
import executors.JUnitExecutors
import executors.JavaRunnerExecutor
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassReader.*
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

@Component
class KotlinCompiler(
    private val kotlinEnvironment: KotlinEnvironment,
    private val javaExecutor: JavaExecutor,
    private val librariesFile: LibrariesFile,
    @Value("\${policy.file}") private val policyFileName: String,
    private val compilerArgumentsUtil: CompilerArgumentsUtil,
    private val jvmCompilerArguments: Set<ExtendedCompilerArgument>,
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

    fun run(
        files: List<ProjectFile>,
        addByteCode: Boolean,
        args: String,
        userCompilerArguments: Map<String, Any>
    ): JvmExecutionResult {
        return execute(files, addByteCode, userCompilerArguments) { output, compiled ->
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

    fun test(
        files: List<ProjectFile>,
        addByteCode: Boolean,
        userCompilerArguments: Map<String, Any>
    ): JvmExecutionResult {
        return execute(files, addByteCode, userCompilerArguments) { output, _ ->
            val mainClass = JUnitExecutors::class.java.name
            javaExecutor.execute(argsFrom(mainClass, output, listOf(output.path.toString())))
                .asJUnitExecutionResult()
        }
    }

    fun compile(files: List<ProjectFile>, userCompilerArguments: Map<String, Any>): CompilationResult<JvmClasses> =
        usingTempDirectory { inputDir ->
            val ioFiles = files.writeToIoFiles(inputDir)
            usingTempDirectory { outputDir ->
                val arguments = ioFiles.map { it.absolutePathString() } +
                        compilerArgumentsUtil.convertCompilerArgumentsToCompilationString(
                            jvmCompilerArguments,
                            compilerArgumentsUtil.PREDEFINED_JVM_ARGUMENTS,
                            userCompilerArguments
                        )
                val result = compileWithToolchain(inputDir, outputDir, arguments)
                return@usingTempDirectory result
            }
        }

    @OptIn(ExperimentalPathApi::class, ExperimentalBuildToolsApi::class, ExperimentalBuildToolsApi::class)
    private fun compileWithToolchain(
        inputDir: Path,
        outputDir: Path,
        arguments: List<String>
    ): CompilationResult<JvmClasses> {
        val sources = inputDir.listDirectoryEntries()

        val logger = CompilationLogger()
        logger.compilationLogs = sources
            .filter { it.name.endsWith(".kt") }
            .associate { it.name to mutableListOf() }

        val toolchains = KotlinToolchains.loadImplementation(this::class.java.classLoader)
        val jvmToolchain = toolchains.getToolchain(JvmPlatformToolchain::class.java)
        val operation = jvmToolchain.createJvmCompilationOperation(sources, outputDir)
        operation.compilerArguments.applyArgumentStrings(arguments)

        toolchains.createBuildSession().use { session ->
            val result = try {
                session.executeOperation(operation, toolchains.createInProcessExecutionPolicy(), logger)
            } catch (e: Exception) {
                throw Exception("Exception executing compilation operation", e)
            }
            return toCompilationResult(result, logger, outputDir)
        }
    }

    private fun toCompilationResult(
        buildResult: org.jetbrains.kotlin.buildtools.api.CompilationResult,
        logger: CompilationLogger,
        outputDir: Path,
    ): CompilationResult<JvmClasses> = when (buildResult) {
        org.jetbrains.kotlin.buildtools.api.CompilationResult.COMPILATION_SUCCESS -> {
            val compilerDiagnostics = CompilerDiagnostics(logger.compilationLogs)
            val outputFiles = buildMap {
                outputDir.visitFileTree {
                    onVisitFile { file, _ ->
                        put(file.relativeTo(outputDir).pathString, file.readBytes())
                        FileVisitResult.CONTINUE
                    }
                }
            }
            Compiled(
                compilerDiagnostics = compilerDiagnostics,
                result = JvmClasses(
                    files = outputFiles,
                    mainClasses = findMainClasses(outputFiles),
                )
            )
        }

        else -> NotCompiled(CompilerDiagnostics(logger.compilationLogs))
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
        userCompilerArguments: Map<String, Any>,
        block: (output: OutputDirectory, compilation: JvmClasses) -> JvmExecutionResult
    ): JvmExecutionResult = try {
        when (val compilationResult = compile(files, userCompilerArguments)) {
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
