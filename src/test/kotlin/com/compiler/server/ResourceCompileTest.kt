package com.compiler.server

import com.compiler.server.base.*
import com.compiler.server.generator.ErrorsChunk
import com.compiler.server.model.ErrorDescriptor
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ResourceCompileTest : BaseExecutorTest() {

  private val testDir = "src/test/resources/test-compile-data"
  private val testFiles = File(testDir).listFiles()

  @TestCompiler
  fun `compile test from resource folder`() {
    assertNotNull(testFiles) { "Can not init test directory" }
    assertTrue(testFiles.isNotEmpty(), "No files in test directory")
    val badMap = mutableMapOf<String, String>()
    testFiles.forEach { file ->
      val code = file.readText()

      val jvmResultErrors = when(val result = run(code, "")) {
        is SynchronousResult -> result.result.errors
        is StreamingResult -> (result.result.first() as ErrorsChunk).errors
      }
      val errorsJvm = validateErrors(jvmResultErrors)
      if (errorsJvm != null) badMap[file.name + ":JVM"] = errorsJvm

      val jsResult = translateToJs(code)
      val errorsJs = validateErrors(jsResult.errors)
      if (errorsJs != null) badMap[file.name + ":JS"] = errorsJs
    }
    if (badMap.isNotEmpty()) {
      error("Compile tests failed. \n Results: ${badMap.entries.joinToString("\n") { "File: ${it.key}. Error: ${it.value}" }}")
    }
  }

  private fun validateErrors(errors: Map<String, List<ErrorDescriptor>>): String? {
    val errs = errors.map { it.value }.flatten()
    if (errs.isNotEmpty()) {
      return renderErrorDescriptors(errs)
    }
    return null
  }
}
