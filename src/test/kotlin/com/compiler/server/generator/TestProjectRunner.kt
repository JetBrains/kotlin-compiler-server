package com.compiler.server.generator

import com.compiler.server.base.filterOnlyErrors
import com.compiler.server.base.hasErrors
import com.compiler.server.base.renderErrorDescriptors
import com.compiler.server.model.*
import com.compiler.server.service.KotlinProjectExecutor
import model.Completion
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.IOException
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.assertTrue


@Component
class TestProjectRunner {
  @Autowired
  private lateinit var kotlinProjectExecutor: KotlinProjectExecutor

  fun run(
    @Language("kotlin")
    code: String,
    contains: String,
    args: String = "",
    addByteCode: Boolean,
  ): ExecutionResult {
    val project = generateSingleProject(text = code, args = args)
    return runAndTest(project, contains, addByteCode)
  }

  fun multiRun(code: List<String>, contains: String, addByteCode: Boolean) {
    val project = generateMultiProject(*code.toTypedArray())
    runAndTest(project, contains, addByteCode)
  }

  fun runJs(
    @Language("kotlin")
    code: String,
    contains: String,
    args: String = "",
    convert: KotlinProjectExecutor.(Project) -> TranslationResultWithJsCode
  ) {
    val project = generateSingleProject(text = code, args = args, projectType = ProjectType.JS_IR)
    convertAndTest(project, contains, convert)
  }

  fun multiRunJs(
    code: List<String>,
    contains: String,
    convert: KotlinProjectExecutor.(Project) -> TranslationResultWithJsCode
  ) {
    val project = generateMultiProject(*code.toTypedArray(), projectType = ProjectType.JS_IR)
    convertAndTest(project, contains, convert)
  }

  fun runWasm(
    @Language("kotlin")
    code: String,
    contains: String,
  ) {
    val project = generateSingleProject(text = code, projectType = ProjectType.WASM)
    convertWasmAndTest(project, contains)
  }

  fun translateToJsIr(@Language("kotlin") code: String): TranslationResultWithJsCode {
    val project = generateSingleProject(text = code, projectType = ProjectType.JS_IR)
    return kotlinProjectExecutor.convertToJsIr(
      project,
    )
  }

  fun runWithException(
    @Language("kotlin")
    code: String,
    contains: String,
    message: String? = null,
    addByteCode: Boolean
  ): ExecutionResult {
    val project = generateSingleProject(text = code)
    val result = kotlinProjectExecutor.run(project, addByteCode)
    Assertions.assertNotNull(result.exception, "Test result should no be a null")
    Assertions.assertTrue(
      result.exception?.fullName?.contains(contains) == true,
      "Actual: ${result.exception?.message}, Expected: $contains"
    )
    if (message != null) Assertions.assertEquals(message, result.exception?.message)
    return result
  }

  fun test(@Language("kotlin") vararg test: String, addByteCode: Boolean): List<TestDescription> {
    val project = generateMultiProject(*test, projectType = ProjectType.JUNIT)
    val result = kotlinProjectExecutor.test(project, addByteCode) as? JunitExecutionResult
    Assertions.assertNotNull(result?.testResults, "Test result should no be a null")
    return result?.testResults?.values?.flatten() ?: emptyList()
  }

  fun testRaw(@Language("kotlin") vararg test: String, addByteCode: Boolean): JunitExecutionResult? =
    executeTest(*test, addByteCode = addByteCode)

  fun complete(
    @Language("kotlin")
    code: String,
    line: Int,
    character: Int,
    completions: List<String>,
    isJs: Boolean = false
  ) {
    val type = if (isJs) ProjectType.JS_IR else ProjectType.JAVA
    val project = generateSingleProject(text = code, projectType = type)
    val result = kotlinProjectExecutor.complete(project, line, character)
      .map { it.displayText }
    Assertions.assertTrue(result.isNotEmpty())
    completions.forEach { suggest ->
      Assertions.assertTrue(result.contains(suggest))
    }
  }

  fun getCompletions(
    @Language("kotlin")
    code: String,
    line: Int,
    character: Int,
    isJs: Boolean = false
  ): List<Completion> {
    val type = if (isJs) ProjectType.JS_IR else ProjectType.JAVA
    val project = generateSingleProject(text = code, projectType = type)
    return kotlinProjectExecutor.complete(project, line, character)
  }

  fun highlight(@Language("kotlin") code: String): CompilerDiagnostics {
    val project = generateSingleProject(text = code)
    return kotlinProjectExecutor.highlight(project)
  }

  fun highlightJS(@Language("kotlin") code: String): CompilerDiagnostics {
    val project = generateSingleProject(text = code, projectType = ProjectType.JS_IR)
    return kotlinProjectExecutor.highlight(project)
  }

  fun highlightWasm(@Language("kotlin") code: String): CompilerDiagnostics {
    val project = generateSingleProject(text = code, projectType = ProjectType.WASM)
    return kotlinProjectExecutor.highlight(project)
  }

  fun getVersion() = kotlinProjectExecutor.getVersion().version

  private fun executeTest(@Language("kotlin") vararg test: String, addByteCode: Boolean): JunitExecutionResult? {
    val project = generateMultiProject(*test, projectType = ProjectType.JUNIT)
    return kotlinProjectExecutor.test(project, addByteCode) as? JunitExecutionResult
  }

  private fun runAndTest(project: Project, contains: String, addByteCode: Boolean): ExecutionResult {
    val result = kotlinProjectExecutor.run(project, addByteCode)
    Assertions.assertNotNull(result, "Test result should no be a null")
    Assertions.assertTrue(
      result.text.contains(contains), """
      Actual: ${result.text} 
      Expected: $contains       
      Result: ${result.compilerDiagnostics}
    """.trimIndent()
    )
    return result
  }

  private fun convertAndTest(
    project: Project,
    contains: String,
    convert: KotlinProjectExecutor.(Project) -> TranslationResultWithJsCode
  ) {
    val result = kotlinProjectExecutor.convert(project)
    Assertions.assertNotNull(result, "Test result should no be a null")
    Assertions.assertFalse(result.hasErrors) {
      "Test contains errors!\n\n" + renderErrorDescriptors(result.compilerDiagnostics.filterOnlyErrors)
    }
    Assertions.assertTrue(result.jsCode!!.contains(contains), "Actual: ${result.jsCode}. \n Expected: $contains")
  }

  private fun convertWasmAndTest(
    project: Project,
    contains: String,
  ): ExecutionResult {
    val result = kotlinProjectExecutor.convertToWasm(
      project,
      debugInfo = true,
    )

    if (result !is TranslationWasmResult) {
      Assertions.assertFalse(result.hasErrors) {
        "Test contains errors!\n\n" + renderErrorDescriptors(result.compilerDiagnostics.filterOnlyErrors)
      }
    }

    result as TranslationWasmResult

    Assertions.assertNotNull(result, "Test result should no be a null")

    val tmpDir = createTempDirectory()
    val jsMain = tmpDir.resolve("moduleId.mjs")
    jsMain.writeText(result.jsInstantiated)
    val jsUninstantiated = tmpDir.resolve("moduleId.uninstantiated.mjs")
    jsUninstantiated.writeText(result.jsCode!!)
    val wasmMain = tmpDir.resolve("moduleId.wasm")
    wasmMain.writeBytes(result.wasm)

    // It is necessary because wasm DCE leaves skiko references
//    Path(this::class.java.classLoader.getResource("wasm-resources/skiko.mjs")!!.path)
//      .copyTo(tmpDir.resolve("skiko.mjs"))

    val wat = result.wat
    val maxWatLengthInMessage = 97
    val formattedWat =
      wat?.let { if (it.length > maxWatLengthInMessage) "${it.take(maxWatLengthInMessage)}..." else it }
    assertTrue(
      actual = wat != null && wat.dropWhile { it.isWhitespace() }.startsWith("(module"),
      message = "wat is expected to start with \"(module\" but is $formattedWat"
    )

    val textResult = startNodeJsApp(
      System.getenv("kotlin.wasm.node.path"),
      jsMain.normalize().absolutePathString()
    )

    tmpDir.toFile().deleteRecursively()

    Assertions.assertTrue(textResult.contains(contains), "Actual: ${textResult}. \n Expected: $contains")
    return result
  }

  @Throws(IOException::class, InterruptedException::class)
  fun startNodeJsApp(
    pathToBinNode: String?,
    pathToAppScript: String?
  ): String {
    val processBuilder = ProcessBuilder()
    processBuilder.command(pathToBinNode, pathToAppScript)
    val process = processBuilder.start()
    val inputStream = process.inputStream
    process.waitFor()
    return inputStream.reader().readText()
  }
}
