package com.compiler.server.compiler.components

import com.compiler.server.model.*
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

sealed class CompilationResult<out T> {
  abstract val compilerDiagnostics: CompilerDiagnostics

  fun <R> flatMap(action: (T) -> CompilationResult<R>): CompilationResult<R> = when (this) {
    is Compiled -> {
      val innerResult = action(result)
      val newDiagnostics = (compilerDiagnostics.map.keys + innerResult.compilerDiagnostics.map.keys).associateWith {
        val l1 = compilerDiagnostics.map[it]
        val l2 = innerResult.compilerDiagnostics.map[it]
        if (l1 != null && l2 != null) l1 + l2 else (l1 ?: l2)!!
      }.let(::CompilerDiagnostics)
      when (innerResult) {
        is Compiled -> innerResult.copy(compilerDiagnostics = newDiagnostics)
        is NotCompiled -> innerResult.copy(compilerDiagnostics = newDiagnostics)
      }
    }
    is NotCompiled -> this
  }
  fun <R> map(action: (T) -> R): CompilationResult<R> = when (this) {
    is Compiled -> Compiled(compilerDiagnostics, action(result))
    is NotCompiled -> this
  }
}

data class Compiled<T>(override val compilerDiagnostics: CompilerDiagnostics, val result: T) : CompilationResult<T>()

data class NotCompiled(override val compilerDiagnostics: CompilerDiagnostics) : CompilationResult<Nothing>()

@OptIn(ExperimentalPathApi::class)
fun <T> usingTempDirectory(action: (path: Path) -> T): T {
  val path = getTempDirectory()
  path.createDirectories()
  return try {
    action(path)
  } finally {
    path.deleteRecursively()
  }
}

private fun getTempDirectory(): Path {
  val dir = System.getProperty("java.io.tmpdir")
  val sessionId = UUID.randomUUID().toString().replace("-", "")
  return File(dir).canonicalFile.resolve(sessionId).toPath()
}

fun List<ProjectFile>.writeToIoFiles(inputDir: Path): List<Path> {
  val ioFiles = map { inputDir / it.name }
  for ((ioFile, projectFile) in ioFiles zip this) {
    ioFile.writeText(projectFile.text)
  }
  return ioFiles
}

val PATH_SEPARATOR: String = File.pathSeparator
