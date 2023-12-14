package com.compiler.server.compiler.components

import com.compiler.server.compiler.components.IndexationProvider.Companion.UNRESOLVED_REFERENCE_PREFIX
import com.compiler.server.model.CompilerDiagnostics
import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ProjectSeveriry
import com.compiler.server.model.TextInterval
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.*

private fun minusOne(value: Int) = if (value > 0) value - 1 else value

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

fun CLICompiler<*>.tryCompilation(inputDirectory: Path, inputFiles: List<Path>, arguments: List<String>): CompilationResult<Unit> = tryCompilation(inputDirectory, inputFiles, arguments) {}

fun <T> CLICompiler<*>.tryCompilation(inputDirectory: Path, inputFiles: List<Path>, arguments: List<String>, onSuccess: () -> T): CompilationResult<T> {
  fun Path.outputFilePathString() = inputDirectory.relativize(this).pathString

  val diagnosticsMap = mutableMapOf<String, MutableList<ErrorDescriptor>>().apply {
    inputFiles.forEach { put(it.outputFilePathString(), mutableListOf()) }
  }
  val defaultFileName = inputFiles.singleOrNull()?.outputFilePathString() ?: ""
  val exitCode = CLITool.doMainNoExit(this, arguments.toTypedArray(), object : MessageRenderer {
    override fun renderPreamble(): String = ""

    override fun render(
      severity: CompilerMessageSeverity,
      message: String,
      location: CompilerMessageSourceLocation?
    ): String {
      val textInterval = location?.let {
        TextInterval(
          start = TextInterval.TextPosition(minusOne(location.line), minusOne(location.column)),
          end = TextInterval.TextPosition(minusOne(location.lineEnd), minusOne(location.columnEnd))
        )
      }
      val messageSeverity: ProjectSeveriry = when (severity) {
        EXCEPTION, ERROR -> ProjectSeveriry.ERROR
        STRONG_WARNING, WARNING -> ProjectSeveriry.WARNING
        INFO, LOGGING, OUTPUT -> ProjectSeveriry.INFO
      }
      val errorFilePath = location?.path?.let(::Path)?.outputFilePathString() ?: defaultFileName

      val errorDescriptor =
        ErrorDescriptor(textInterval, message, messageSeverity, className = messageSeverity.name.let {
          when {
            !message.startsWith(UNRESOLVED_REFERENCE_PREFIX) && severity == ERROR -> "red_wavy_line"
            else -> it
          }
        })

      diagnosticsMap.getOrPut(errorFilePath) { mutableListOf() }.add(errorDescriptor)
      return ""
    }

    override fun renderUsage(usage: String): String =
      render(STRONG_WARNING, usage, null)

    override fun renderConclusion(): String = ""

    override fun getName(): String = "Redirector"
  })
  val diagnostics = CompilerDiagnostics(diagnosticsMap)
  return when {
    diagnostics.any { it.severity == ProjectSeveriry.ERROR } -> NotCompiled(diagnostics)
    exitCode.code != 0 -> ErrorDescriptor(
      severity = ProjectSeveriry.ERROR,
      message = "Compiler finished with non-null exit code ${exitCode.code}: ${exitCode.name}",
      interval = null
    ).let { NotCompiled(CompilerDiagnostics(mapOf(defaultFileName to listOf(it)))) }

    else -> Compiled(result = onSuccess(), compilerDiagnostics = diagnostics)
  }
}

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
  return Paths.get(dir, sessionId)
}

fun List<KtFile>.writeToIoFiles(inputDir: Path): List<Path> {
  val ioFiles = map { inputDir / it.name }
  for ((ioFile, ktFile) in ioFiles zip this) {
    ioFile.writeText(ktFile.text)
  }
  return ioFiles
}

val PATH_SEPARATOR: String = java.io.File.pathSeparator
