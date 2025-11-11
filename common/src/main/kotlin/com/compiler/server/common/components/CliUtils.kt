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
  val dir = System.getProperty("java.io.tmpdir")
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
  val additionalCompilerArgumentsForKLib: List<String> = mutableListOf(
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
    multiModule: Boolean,
    outputDir: Path,
    debugInfo: Boolean,
): List<String> {
  return mutableListOf(
    "-Xreport-all-warnings",
    "-Wextra",
    "-Xwasm",
    "-Xir-produce-js",
    "-Xinclude=$klibPath",
    "-libraries=${(dependencies + klibPath).joinToString(PATH_SEPARATOR)}",
    "-ir-output-dir=${(outputDir / "wasm").toFile().canonicalPath}",
    "-ir-output-name=$moduleName",
    "-Xwasm-debug-friendly",
  ).also {
    if (debugInfo) it.add("-Xwasm-generate-wat")

    if (multiModule) {
        it.add("-Xwasm-included-module-only")
    } else {
      it.add("-Xir-dce")
    }
  }
}

val PATH_SEPARATOR: String = File.pathSeparator
