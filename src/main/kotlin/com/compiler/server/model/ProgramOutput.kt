package com.compiler.server.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import executors.ThrowableSerializer

val outputMapper = ObjectMapper().apply {
  registerModule(SimpleModule().apply {
    addSerializer(Throwable::class.java, ThrowableSerializer())
  })
}

const val ERROR_STREAM_START = "<errStream>"
const val ERROR_STREAM_END = "</errStream>"

data class ProgramOutput(
  val standardOutput: String = "",
  val exception: Exception? = null,
  val restriction: String? = null
) {
  fun asExecutionResult(): ExecutionResult {
    return when {
      restriction != null -> ExecutionResult().apply { text = buildRestriction(restriction) }
      exception != null -> ExecutionResult(exception = exception.toExceptionDescriptor())
      standardOutput.isBlank() -> ExecutionResult()
      else -> {
        try {
          // coroutines can produced incorrect output. see example in `base coroutines test 7`
          if (standardOutput.startsWith("{")) outputMapper.readValue(standardOutput, ExecutionResult::class.java)
          else {
            val result = outputMapper.readValue("{" + standardOutput.substringAfter("{"), ExecutionResult::class.java)
            result.apply {
              text = standardOutput.substringBefore("{") + text
            }
          }
        } catch (e: Exception) {
          ExecutionResult(exception = e.toExceptionDescriptor())
        }
      }
    }
  }

  fun asJUnitExecutionResult(): JunitExecutionResult {
    return when {
      restriction != null -> JunitExecutionResult().apply { text = buildRestriction(restriction) }
      exception != null -> JunitExecutionResult().also { it.exception = exception.toExceptionDescriptor() }
      else -> {
        val result = outputMapper.readValue(
          standardOutput,
          object : TypeReference<Map<String, List<TestDescription>>>() {}
        )
        JunitExecutionResult(result)
      }
    }
  }

  private fun buildRestriction(restriction: String) = "$ERROR_STREAM_START$restriction${ERROR_STREAM_END}"
}