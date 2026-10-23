package com.compiler.server.compiler.components

import com.compiler.server.model.*
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText

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
