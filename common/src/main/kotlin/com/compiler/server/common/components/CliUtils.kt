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
    icDir: Path?,
    log: (String) -> Unit,
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
  ).also {
      if (icDir != null) {
          typeInfoArg(icDir, log)?.let { stdlibTypeInfoArg ->
              it.add(stdlibTypeInfoArg)
          }
      }
  } + compilerPluginsArgs

  return filePaths + additionalCompilerArgumentsForKLib
}

fun linkWasmArgs(
    moduleName: String,
    klibPath: String,
    dependencies: List<String>,
    icDir: Path?,
    outputDir: Path,
    debugInfo: Boolean,
    log: (String) -> Unit,
): List<String> {
  return mutableListOf(
    "-Xreport-all-warnings",
    "-Wextra",
    "-Xwasm",
    "-Xir-produce-js",
    "-Xinclude=$klibPath",
    "-libraries=${dependencies.joinToString(PATH_SEPARATOR)}",
    "-ir-output-dir=${(outputDir / "wasm").toFile().canonicalPath}",
    "-ir-output-name=$moduleName",
  ).also {
    if (debugInfo) it.add("-Xwasm-generate-wat")

    if (icDir != null) {
        typeInfoArg(icDir, log)?.let { stdlibTypeInfoArg ->
            it.add(stdlibTypeInfoArg)
        }
    } else {
      it.add("-Xir-dce")
    }
  }
}

private fun typeInfoArg(
    icDir: Path,
    log: (String) -> Unit,
): String? {
    val allTypeInfoFiles = icDir.toFile().listFiles() ?: run {
        log("No typeinfo files in $icDir, probably you need to run :cache-maker:prepareTypeInfoIntoComposeWasmCache task")
        return null
    }

    val stdlibTypeInfo = allTypeInfoFiles
        .firstOrNull { file -> file.name.endsWith(".typeinfo.bin") }

    if (stdlibTypeInfo == null) {
        log("No typeinfo files in $icDir, probably you need to run :cache-maker:prepareTypeInfoIntoComposeWasmCache task")
        return null
    }

    if (allTypeInfoFiles.size > 1) {
        log("There are more than 1 typeinfo files in $icDir: ${allTypeInfoFiles.joinToString(", ") { it.name }}")
    }

    return "-Xwasm-typeinfo-file=${stdlibTypeInfo.normalize().absolutePath}"
}

val PATH_SEPARATOR: String = File.pathSeparator
