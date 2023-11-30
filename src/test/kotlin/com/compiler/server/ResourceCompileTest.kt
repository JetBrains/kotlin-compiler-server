package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.renderErrorDescriptors
import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ProjectSeveriry
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ResourceCompileTest : BaseExecutorTest() {

  private val testDirJVM = "src/test/resources/test-compile-data/jvm"
  private val testDirJS = "src/test/resources/test-compile-data/js"
  private val testFilesJVM = File(testDirJVM).walk().toList().filter { it.isFile }
  private val testFilesJS = File(testDirJS).walk().toList().filter { it.isFile }

  @Test
  fun `compile test from resource folder`() {
    assertNotNull(testFilesJVM) { "Can not init test directory" }
    assertTrue(testFilesJVM.isNotEmpty(), "No files in test directory")
    val badMap = mutableMapOf<String, String>()

    testFilesJVM.forEach { file ->
      val code = file.readText()

      val jvmResult = run(code, "")
      val errorsJvm = validateErrors(jvmResult.compilerDiagnostics)
      if (errorsJvm != null) badMap[file.path + ":JVM"] = errorsJvm
    }

    assertNotNull(testFilesJS) { "Can not init test directory" }
    assertTrue(testFilesJS.isNotEmpty(), "No files in test directory")

    testFilesJS.forEach { file ->
      val code = file.readText()

      val jsResult = translateToJsIr(code)
      val errorsJs = validateErrors(jsResult.compilerDiagnostics)
      if (errorsJs != null) badMap[file.path + ":JS"] = errorsJs
    }

    if (badMap.isNotEmpty()) {
      error("Compile tests failed. \n Results: ${badMap.entries.joinToString("\n") { "File: ${it.key}. Error: ${it.value}" }}")
    }
  }

  private fun validateErrors(errors: List<ErrorDescriptor>): String? {
    val errs = errors.filter { it.severity == ProjectSeveriry.ERROR }
    if (errs.isNotEmpty()) {
      return renderErrorDescriptors(errs)
    }
    return null
  }
}
