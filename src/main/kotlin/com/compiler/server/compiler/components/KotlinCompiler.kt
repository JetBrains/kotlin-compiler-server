package com.compiler.server.compiler.components

import com.compiler.server.executor.CommandLineArgument
import com.compiler.server.executor.JavaExecutor
import com.compiler.server.model.CompilerDiagnostics
import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.JvmExecutionResult
import com.compiler.server.model.OutputDirectory
import com.compiler.server.model.ProjectFile
import com.compiler.server.model.ProjectSeveriry
import com.compiler.server.model.TextInterval
import com.compiler.server.model.bean.LibrariesFile
import com.compiler.server.model.toExceptionDescriptor
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import kotlin.collections.plus
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

                // Process output files
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
            // Log the exception for debugging
            println("Error using kotlin-build-tools-api: ${e.message}")
            e.printStackTrace()

            // Return null to indicate that we should fall back to the old approach
            return null
        }
    }

    @OptIn(ExperimentalPathApi::class, ExperimentalBuildToolsApi::class)
    private fun compileWithCompilationService(
        files: List<ProjectFile>,
        inputDir: Path,
        outputDir: Path,
        cp: String
    ): CompilationResult<JvmClasses>? {
        try {
            val service = CompilationService.loadImplementation(ClassLoader.getSystemClassLoader())
            val projectId = ProjectId.RandomProjectUUID()
            val strategyConfig = service.makeCompilerExecutionStrategyConfiguration().useInProcessStrategy()
            val compilationConfig = service.makeJvmCompilationConfiguration()

            val ioFiles = files.writeToIoFiles(inputDir)

            val arguments =
                ioFiles.map { it.absolutePathString() } + KotlinEnvironment.additionalCompilerArguments + listOf(
                    "-cp", kotlinEnvironment.classpath.joinToString(PATH_SEPARATOR) { it.absolutePath },
                    "-module-name", "web-module",
                    "-no-stdlib", "-no-reflect",
                    "-progressive",
                    "-d", outputDir.absolutePathString(),
                ) + kotlinEnvironment.compilerPlugins.map { plugin -> "-Xplugin=${plugin.absolutePath}" }

            val sources = inputDir.listDirectoryEntries().map { it.toFile() }
            var warnings : Map<String, List<ErrorDescriptor>> = emptyMap()

            val outputStream = ByteArrayOutputStream()
            val printStream = PrintStream(outputStream)
            val originalOut = System.out
            val originalErr = System.err
            System.setOut(printStream)
            System.setErr(printStream)
            try {
                val result = service.compileJvm(projectId, strategyConfig, compilationConfig, sources, arguments)
                val rawOutput = outputStream.toString()
                val output = rawOutput.replace(
                    Regex(
                        """w: ATTENTION![\s\S]*?Use it at your own risk!\n\nw: Following manually enabled features will force generation of pre-release binaries: ExplicitBackingFields\nw: Duplicate source root: [^\n]*\n"""
                    ),
                    ""
                )

                // Parse diagnostics that may span multiple lines. Only consider blocks starting with "e:" or "w:".
                val lines = output.lines()
                val blocks = mutableListOf<String>()
                val current = StringBuilder()
                fun flush() {
                    if (current.isNotEmpty()) {
                        blocks += current.toString().trimEnd()
                        current.setLength(0)
                    }
                }
                for (line in lines) {
                    if (line.startsWith("e: ") || line.startsWith("w: ")) {
                        flush()
                        current.append(line)
                    } else {
                        // Append continuation lines to the current block if any
                        if (current.isNotEmpty()) {
                            current.append('\n').append(line)
                        }
                    }
                }
                flush()

                for (block in blocks) {
                    try {
                        // First token is severity symbol; next token should be a path with line:col, but message may contain spaces/newlines
                        val firstSpace = block.indexOf(' ')
                        if (firstSpace == -1) continue
                        val typeSymbol = block.substring(0, firstSpace)
                        val severity = when (typeSymbol) {
                            "e:" -> ProjectSeveriry.ERROR
                            "w:" -> ProjectSeveriry.WARNING
                            else -> ProjectSeveriry.INFO
                        }
                        if (severity == ProjectSeveriry.INFO) continue // ignore anything not starting with e:/w:

                        val rest = block.substring(firstSpace + 1)
                        // Path is the first whitespace-separated token in rest
                        val secondSpace = rest.indexOf(' ')
                        if (secondSpace == -1) continue
                        val path = rest.substring(0, secondSpace)
                        val message = rest.substring(secondSpace + 1).trimStart()

                        val className = when (typeSymbol) {
                            "w:" -> "WARNING"
                            else -> path.substringAfterLast('/').substringBeforeLast('.')
                        }

                        val splitPath = path.split(":")
                        if (splitPath.size < 3) continue
                        val line = splitPath[splitPath.size - 2].toIntOrNull()?.minus(1) ?: continue
                        val ch = splitPath[splitPath.size - 1].toIntOrNull()?.minus(1) ?: 0

                        val ed = ErrorDescriptor(
                            TextInterval(
                                TextInterval.TextPosition(line, ch),
                                TextInterval.TextPosition(line, ch)
                            ), message, severity, className
                        )
                        warnings = warnings + (path to ((warnings[path] ?: emptyList()) + ed))
                    } catch (_: Exception) {
                        // ignore malformed lines
                    }
                }

                println("Test:$output")


                // Process output files
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
                        val lw = warnings
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
                        NotCompiled(CompilerDiagnostics(warnings))
                    }
                }
            } finally {
                System.setOut(originalOut)
                System.setErr(originalErr)
                service.finishProjectCompilation(projectId)
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
    fun compile(files: List<ProjectFile>): CompilationResult<JvmClasses> = usingTempDirectory { inputDir ->


        files.writeToIoFiles(inputDir)
        usingTempDirectory { outputDir ->

            val classpath = kotlinEnvironment.classpath.joinToString(PATH_SEPARATOR) { it.absolutePath }
//            val newApiResult = compileWithToolchain(inputDir, outputDir, classpath)
            val newApiResult = compileWithCompilationService(files, inputDir, outputDir, classpath)

            // If the new approach succeeded, return its result
            if (newApiResult != null) {
                println("Successfully compiled with kotlin-build-tools-api")
                return@usingTempDirectory newApiResult
            }

            return@usingTempDirectory NotCompiled(
                CompilerDiagnostics(
//          mapOf("null" to listOf(ErrorDescriptor(null, "Failed to compile using kotlin-build-tools-api",
//            ProjectSeveriry.ERROR, null)))
                )
            )
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
