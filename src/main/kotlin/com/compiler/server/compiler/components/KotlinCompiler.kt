package com.compiler.server.compiler.components

import com.compiler.server.executor.CommandLineArgument
import com.compiler.server.executor.JavaExecutor
import com.compiler.server.model.*
import com.compiler.server.model.bean.LibrariesFile
import component.KotlinEnvironment
import executors.JUnitExecutors
import executors.JavaRunnerExecutor
import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchain
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
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
    private fun compileWithToolchain(inputDir: Path, outputDir: Path, cp: String): CompilationResult<JvmClasses>? {
        try {
            val sources = inputDir.listDirectoryEntries()
            val toolchain = KotlinToolchain.loadImplementation(ClassLoader.getSystemClassLoader())
            val operation = toolchain.jvm.createJvmCompilationOperation(sources, outputDir)
            operation.compilerArguments[JvmCompilerArguments.JvmCompilerArgument<String?>("CLASSPATH")] = cp
            operation.compilerArguments[JvmCompilerArguments.JvmCompilerArgument<String?>("MODULE_NAME")] = "web-module"
            operation.compilerArguments[JvmCompilerArguments.JvmCompilerArgument<Boolean>("NO_STDLIB")] = true
            operation.compilerArguments[JvmCompilerArguments.JvmCompilerArgument<Boolean>("NO_Reflect")] = true
            operation.compilerArguments[JvmCompilerArguments.JvmCompilerArgument<Boolean>("PROGRESSIVE")] = true

            val optIns = listOf(
                "kotlin.ExperimentalStdlibApi",
                "kotlin.time.ExperimentalTime",
                "kotlin.RequiresOptIn",
                "kotlin.ExperimentalUnsignedTypes",
                "kotlin.contracts.ExperimentalContracts",
                "kotlin.experimental.ExperimentalTypeInference",
                "kotlin.uuid.ExperimentalUuidApi",
                "kotlin.io.encoding.ExperimentalEncodingApi",
                "kotlin.concurrent.atomics.ExperimentalAtomicApi",
            )
            operation.compilerArguments[JvmCompilerArguments.JvmCompilerArgument<List<String>>("OPT_IN")] = optIns

            // --- -X... przełączniki (bez wartości -> boolean) ---
            operation.compilerArguments[JvmCompilerArguments.JvmCompilerArgument("X_CONTEXT_PARAMETERS")] = true
            operation.compilerArguments[JvmCompilerArguments.JvmCompilerArgument("X_NESTED_TYPE_ALIASES")] = true
            operation.compilerArguments[JvmCompilerArguments.JvmCompilerArgument("X_REPORT_ALL_WARNINGS")] = true
            operation.compilerArguments[JvmCompilerArguments.JvmCompilerArgument("X_EXPLICIT_BACKING_FIELDS")] = true

//            "-Wextra",
//            "-XPlugin=kotlinEnvironment.compilerPlugins.map { plugin -> "-Xplugin=${plugin.absolutePath}" }"

            val logger = CompilationLogger()

            val session = toolchain.createBuildSession()

            try {
                val result = session.executeOperation(operation, toolchain.createInProcessExecutionPolicy(), logger)

                val outputFiles = buildMap {
                    outputDir.visitFileTree {
                        onVisitFile { file, _ ->
                            put(file.relativeTo(outputDir).pathString, file.readBytes())
                            FileVisitResult.CONTINUE
                        }
                    }
                }

                val mainClasses = findMainClasses(outputFiles)

                return when (result) {
                    org.jetbrains.kotlin.buildtools.api.CompilationResult.COMPILATION_SUCCESS -> {
                        val lw = logger.warnings
                        val cd = CompilerDiagnostics(lw)
                        Compiled(
                            compilerDiagnostics = cd,
                            result = JvmClasses(
                                files = outputFiles,
                                mainClasses = mainClasses,
                            )
                        )
                    }

                    else -> {
                        NotCompiled(CompilerDiagnostics(logger.warnings))
                    }
                }
            } finally {
                session.close()
            }
        } catch (e: Exception) {
            println("Error using kotlin-build-tools-api: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    @OptIn(ExperimentalPathApi::class, ExperimentalBuildToolsApi::class)
    private fun compileWithCompilationService(
        files: List<ProjectFile>,
        inputDir: Path,
        outputDir: Path,
        cp: String
    ): CompilationResult<JvmClasses> {

        val logger = CompilationLogger()

        val service = CompilationService.loadImplementation(ClassLoader.getSystemClassLoader())
        val projectId = ProjectId.RandomProjectUUID()
        val strategyConfig = service.makeCompilerExecutionStrategyConfiguration().useInProcessStrategy()
        val compilationConfig = service.makeJvmCompilationConfiguration().useLogger(logger)

        val ioFiles = files.writeToIoFiles(inputDir)

        val arguments =
            ioFiles.map { it.absolutePathString() } + KotlinEnvironment.additionalCompilerArguments + listOf(
                "-cp", cp,
                "-module-name", "web-module",
                "-no-stdlib", "-no-reflect",
                "-progressive",
                "-d", outputDir.absolutePathString(),
            ) + kotlinEnvironment.compilerPlugins.map { plugin -> "-Xplugin=${plugin.absolutePath}" }

        val sources = inputDir.listDirectoryEntries().map { it.toFile() }
        val compResult = try {
            service.compileJvm(projectId, strategyConfig, compilationConfig, sources, arguments)
        } catch (_: Exception) {
            null
        }
        val success = compResult == org.jetbrains.kotlin.buildtools.api.CompilationResult.COMPILATION_SUCCESS

        val outputFiles = buildMap {
            outputDir.visitFileTree {
                onVisitFile { file, _ ->
                    put(file.relativeTo(outputDir).pathString, file.readBytes())
                    FileVisitResult.CONTINUE
                }
            }
        }
        
        try {
            val mainClasses = findMainClasses(outputFiles)

            return if (success) {
                val cd = CompilerDiagnostics(logger.warnings)
                Compiled(
                    compilerDiagnostics = cd,
                    result = JvmClasses(
                        files = outputFiles,
                        mainClasses = mainClasses,
                    )
                )
            } else {
                NotCompiled(CompilerDiagnostics(logger.warnings))
            }
        } finally {
            service.finishProjectCompilation(projectId)
        }
    }

    @OptIn(ExperimentalPathApi::class)
    fun compile(files: List<ProjectFile>): CompilationResult<JvmClasses> = usingTempDirectory { inputDir ->
        files.writeToIoFiles(inputDir)
        usingTempDirectory { outputDir ->
            val classpath = kotlinEnvironment.classpath.joinToString(PATH_SEPARATOR) { it.absolutePath }
            // TODO(Zofia Wiora): Switch to compileWithToolchain when all the flags are available
            val result = compileWithCompilationService(files, inputDir, outputDir, classpath)
            return@usingTempDirectory result
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
