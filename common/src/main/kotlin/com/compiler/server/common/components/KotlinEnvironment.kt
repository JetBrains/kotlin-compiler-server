package com.compiler.server.common.components

import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import java.io.File

// NOTE: if new class paths are added, please add them to `JavaExec` task's inputs in build.gradle.kts as well
class KotlinEnvironment(
  val classpath: List<File>,
  additionalJsClasspath: List<File>,
  additionalWasmClasspath: List<File>,
  additionalComposeWasmClasspath: List<File>,
  composeWasmCompilerPlugins: List<File>,
  val compilerPlugins: List<File> = emptyList(),
  composeWasmCompilerPluginsOptions: List<CompilerPluginOption>,
  val dependenciesComposeWasmHash: String = "",
  val dependenciesStaticUrl: String = "",
) {

  val JS_LIBRARIES = additionalJsClasspath
    .map { it.absolutePath }
    .filter { isJsKlib(it) }
  val WASM_LIBRARIES = additionalWasmClasspath
    .map { it.absolutePath }
    .filter { isWasmKlib(it) }
  val COMPOSE_WASM_LIBRARIES = additionalComposeWasmClasspath
    .map { it.absolutePath }
    .filter { isWasmKlib(it) }
  val COMPOSE_WASM_COMPILER_PLUGINS = composeWasmCompilerPlugins
    .map { it.absolutePath }

  val composeWasmCompilerPluginOptions = composeWasmCompilerPluginsOptions
    .map { "plugin:${it.id}:${it.option}=${it.value}" }

    @Synchronized
    fun <T> synchronize(f: () -> T): T {
        return f()
    }

    private fun isJsKlib(path: String) = KlibLoader {
        libraryPaths(path)
        platformChecker(KlibPlatformChecker.JS)
    }.load().librariesStdlibFirst.isNotEmpty()

    private fun isWasmKlib(path: String) = KlibLoader {
        libraryPaths(path)
        platformChecker(KlibPlatformChecker.Wasm())
    }.load().librariesStdlibFirst.isNotEmpty()
}
