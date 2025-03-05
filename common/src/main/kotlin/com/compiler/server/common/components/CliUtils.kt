package com.compiler.server.common.components

import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

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
  val dir = System.getenv("COMPILER_SERVER_TMP_PATH").takeIf {
    if (it.isNullOrBlank()) return@takeIf false
    val directory = File(it)
    directory.exists() && directory.isDirectory
  } ?: System.getProperty("java.io.tmpdir")

  val sessionId = UUID.randomUUID().toString().replace("-", "")
  return File(dir).canonicalFile.resolve(sessionId).toPath()
}

fun compileWasmArgs(
  moduleName: String,
  filePaths: List<String>,
  klibPath: String,
  compilerPlugins: List<String>,
  compilerPluginOptions: List<String>,
  dependencies: List<String>,
): List<String> {
  val compilerPluginsArgs: List<String> = compilerPlugins
    .takeIf { it.isNotEmpty() }
    ?.let { plugins ->
      plugins.map {
        "-Xplugin=$it"
      } + compilerPluginOptions.map {
        "-P=$it"
      }
    } ?: emptyList()
  val additionalCompilerArgumentsForKLib: List<String> = listOf(
    "-Xreport-all-warnings",
    "-Wextra",
    "-Xwasm",
    "-Xir-produce-klib-dir",
    "-libraries=${dependencies.joinToString(PATH_SEPARATOR)}",
    "-ir-output-dir=$klibPath",
    "-ir-output-name=$moduleName",
  ) + compilerPluginsArgs

  return filePaths + additionalCompilerArgumentsForKLib
}

fun linkWasmArgs(
  moduleName: String,
  klibPath: String,
  dependencies: List<String>,
  icDir: Path?,
  outputDir: Path,
  debugInfo: Boolean,
): List<String> {
  return mutableListOf(
    "-Xreport-all-warnings",
    "-Wextra",
    "-Xwasm",
    "-Xir-produce-js",
    "-Xinclude=$klibPath",
    "-libraries=${dependencies.joinToString(PATH_SEPARATOR)}",
    "-ir-output-dir=${(outputDir / "wasm").toFile().canonicalPath}",
    "-ir-output-name=$moduleName",).also {
    if (debugInfo) it.add("-Xwasm-generate-wat")

    if (icDir != null) {
      it.add("-Xcache-directory=${icDir.normalize().absolutePathString()}")
    } else {
      it.add("-Xir-dce")
    }
  }
}

val PATH_SEPARATOR: String = File.pathSeparator
