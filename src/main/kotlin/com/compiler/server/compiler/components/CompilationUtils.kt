package com.compiler.server.compiler.components

import com.compiler.server.model.*
import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.name
import kotlin.io.path.writeText
import org.jetbrains.kotlin.buildtools.api.CompilationResult as BTACompilationResult

sealed class CompilationResult<out T> {
    abstract val compilerDiagnostics: CompilerDiagnostics

    fun <R> map(action: (T) -> R): CompilationResult<R> = when (this) {
        is Compiled -> Compiled(compilerDiagnostics, action(result))
        is NotCompiled -> this
    }
}

data class Compiled<T>(override val compilerDiagnostics: CompilerDiagnostics, val result: T) : CompilationResult<T>()

data class NotCompiled(override val compilerDiagnostics: CompilerDiagnostics) : CompilationResult<Nothing>()

fun List<ProjectFile>.writeToIoFiles(inputDir: Path): List<Path> {
    val ioFiles = map { inputDir / it.name }
    for ((ioFile, projectFile) in ioFiles zip this) {
        ioFile.writeText(projectFile.text)
    }
    return ioFiles
}

@OptIn(ExperimentalBuildToolsApi::class)
fun KotlinToolchains.executeSequentially(
    sources: List<Path>,
    vararg operations: BuildOperation<BTACompilationResult>,
): CompilationResult<Unit> = createBuildSession().use { session ->
    val logger = CompilationLogger(
        compilationLogs = sources
            .filter { it.name.endsWith(".kt") }
            .associate { it.name to mutableListOf() }
    )

    val result = try {
        val executionPolicy = createInProcessExecutionPolicy()
        operations.fold(BTACompilationResult.COMPILATION_SUCCESS) { result, operation ->
            if (result == BTACompilationResult.COMPILATION_SUCCESS) {
                session.executeOperation(operation, executionPolicy, logger)
            } else result
        }
    } catch (e: Exception) {
        throw Exception("Exception executing compilation operation", e)
    }

    val compilerDiagnostics = CompilerDiagnostics(logger.compilationLogs)
    when (result) {
        BTACompilationResult.COMPILATION_SUCCESS -> Compiled(compilerDiagnostics, Unit)
        else -> NotCompiled(compilerDiagnostics)
    }
}
