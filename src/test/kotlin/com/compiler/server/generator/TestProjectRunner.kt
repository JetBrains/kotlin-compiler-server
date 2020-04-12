package com.compiler.server.generator

import com.compiler.server.generator.StreamingJsonChunkUtil.readJsonChunk
import com.compiler.server.model.*
import com.compiler.server.service.KotlinProjectExecutor
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.*
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import kotlin.reflect.KClass

@Component
class TestProjectRunner {
  @Autowired
  lateinit var kotlinProjectExecutor: KotlinProjectExecutor

  fun run(code: String, contains: String, args: String = ""): ExecutionResult {
    val project = generateSingleProject(text = code, args = args)
    return runAndTest(project, contains)
  }

  fun runStreaming(code: String, contains: String, args: String = ""): List<StreamingJsonChunk> {
    val project = generateSingleProject(text = code, args = args)
    return runAndTestStreaming(project, contains)
  }

  fun multiRun(code: List<String>, contains: String) {
    val project = generateMultiProject(*code.toTypedArray())
    runAndTest(project, contains)
  }

  fun multiRunStreaming(code: List<String>, contains: String) {
    val project = generateMultiProject(*code.toTypedArray())
    runAndTestStreaming(project, contains)
  }

  fun runJs(code: String, contains: String, args: String = "") {
    val project = generateSingleProject(text = code, args = args, projectType = ProjectType.JS)
    convertAndTest(project, contains)
  }

  fun multiRunJs(code: List<String>, contains: String) {
    val project = generateMultiProject(*code.toTypedArray(), projectType = ProjectType.JS)
    convertAndTest(project, contains)
  }

  fun runWithException(code: String, contains: String): ExecutionResult {
    val project = generateSingleProject(text = code)
    val result = kotlinProjectExecutor.run(project)
    Assertions.assertNotNull(result.exception, "Test result should no be a null")
    Assertions.assertTrue(
      result.exception?.fullName?.contains(contains) == true,
      "Actual: ${result.exception?.message}, Expected: $contains"
    )
    return result
  }

  fun runWithExceptionStreaming(code: String, contains: String): List<StreamingJsonChunk> {
    val project = generateSingleProject(text = code)
    val jsonChunks = runStreamingAndGetJsonChunks(project)
    val exception = jsonChunks.filterIsInstance(ExceptionChunk::class.java).firstOrNull()?.exception
    Assertions.assertTrue(
        exception?.fullName?.contains(contains) == true,
        "Actual: ${exception?.message}, Expected: $contains"
    )
    return jsonChunks
  }

  fun test(vararg test: String): List<TestDescription> {
    val result = testRaw(*test)
    return result.testResults.values.flatten()
  }

  fun testStreaming(vararg test: String): List<TestDescription> {
    val chunks = testRawStreaming(*test)
    return chunks.filterIsInstance(TestResultChunk::class.java).map { it.testResult }
  }

  fun testRaw(vararg test: String): JunitExecutionResult {
    val project = generateMultiProject(*test, projectType = ProjectType.JUNIT)
    val executionResult = kotlinProjectExecutor.test(project)
    Assertions.assertNotNull(executionResult, "Test execution result should not be null")
    return executionResult as JunitExecutionResult
  }

  fun testRawStreaming(vararg test: String): List<StreamingJsonChunk> {
    val project = generateMultiProject(*test, projectType = ProjectType.JUNIT)
    val outputStream = ByteArrayOutputStream()
    kotlinProjectExecutor.testStreaming(project, outputStream)
    val resultString = String(outputStream.toByteArray())
    return resultString.split("\n\n").filter { it.isNotEmpty() }.map { readJsonChunk(it) }
  }

  fun complete(
    code: String,
    line: Int,
    character: Int,
    completions: List<String>,
    isJs: Boolean = false
  ) {
    val type = if (isJs) ProjectType.JS else ProjectType.JAVA
    val project = generateSingleProject(text = code, projectType = type)
    val result = kotlinProjectExecutor.complete(project, line, character)
      .map { it.displayText }
    Assertions.assertTrue(result.isNotEmpty())
    completions.forEach { suggest ->
      Assertions.assertTrue(result.contains(suggest))
    }
  }

  fun highlight(code: String): Map<String, List<ErrorDescriptor>> {
    val project = generateSingleProject(text = code)
    return kotlinProjectExecutor.highlight(project)
  }

  fun highlightJS(code: String): Map<String, List<ErrorDescriptor>> {
    val project = generateSingleProject(text = code, projectType = ProjectType.JS)
    return kotlinProjectExecutor.highlight(project)
  }

  fun getVersion() = kotlinProjectExecutor.getVersion().version

  private fun runAndTest(project: Project, contains: String): ExecutionResult {
    val result = kotlinProjectExecutor.run(project)
    Assertions.assertNotNull(result, "Test result should no be a null")
    Assertions.assertTrue(result.text.contains(contains) == true, "Actual: ${result.text}. \n Expected: $contains")
    return result
  }

  private fun convertAndTest(project: Project, contains: String) {
    val result = kotlinProjectExecutor.convertToJs(project)
    Assertions.assertNotNull(result, "Test result should no be a null")
    Assertions.assertTrue(result.jsCode!!.contains(contains), "Actual: ${result.jsCode}. \n Expected: $contains")
  }

  private fun runAndTestStreaming(project: Project, contains: String): List<StreamingJsonChunk> {
    val jsonChunks = runStreamingAndGetJsonChunks(project)
    val stdout = jsonChunks
        .filterIsInstance(OutStreamChunk::class.java)
        .map { it.outStream }
        .joinToString(separator = "")

    val stdErr = jsonChunks
        .filterIsInstance(ErrStreamChunk::class.java)
        .map { it.errStream }
        .joinToString(separator = "")

    Assertions.assertTrue(stdout.contains(contains) || stdErr.contains(contains),
        "Actual stdout: ${stdout}\nActual stderr: {$stdErr}\nExpected: $contains")
    return jsonChunks
  }

  private fun runStreamingAndGetJsonChunks(project: Project): List<StreamingJsonChunk> {
    val outputStream = ByteArrayOutputStream()
    kotlinProjectExecutor.runStreaming(project, outputStream)
    val resultString = String(outputStream.toByteArray())
    return resultString.split("\n\n").filter { it.isNotEmpty() }.map { readJsonChunk(it) }
  }
}