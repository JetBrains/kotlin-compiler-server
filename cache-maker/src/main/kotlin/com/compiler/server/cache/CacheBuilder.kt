package com.compiler.server.cache

import com.compiler.server.common.components.KotlinEnvironment
import com.compiler.server.common.components.compileWasmArgs
import com.compiler.server.common.components.linkWasmArgs
import com.compiler.server.common.components.usingTempDirectory
import org.jetbrains.kotlin.cli.common.CLICompiler.Companion.doMainNoExit
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import java.io.File
import kotlin.io.path.div


class CacheBuilder(
  private val kotlinEnvironment: KotlinEnvironment,
) {
  fun compileCache(outputForCache: String) {
    val moduleName = "playground"
    usingTempDirectory { outputDir ->
      val resource = this::class.java.classLoader.getResource("File.kt")!!.path

      val klibPath = (outputDir / "klib").toFile().normalize().absolutePath

      val k2JSCompiler = K2JSCompiler()

      doMainNoExit(
        k2JSCompiler,
        compileWasmArgs(
          moduleName,
          listOf(resource),
          klibPath,
          kotlinEnvironment.COMPOSE_WASM_COMPILER_PLUGINS,
          kotlinEnvironment.composeWasmCompilerPluginOptions,
          kotlinEnvironment.COMPOSE_WASM_LIBRARIES
        ).toTypedArray()
      )

      usingTempDirectory { tmpDir ->
        val cachesDir = tmpDir.resolve("caches").normalize()
        doMainNoExit(
          k2JSCompiler,
          linkWasmArgs(
            moduleName,
            klibPath,
            kotlinEnvironment.COMPOSE_WASM_LIBRARIES,
            cachesDir.normalize(),
            outputDir,
            false
          ).toTypedArray()
        )

        cachesDir.toFile().copyRecursively(File(outputForCache), overwrite = true)
      }
    }
  }
}