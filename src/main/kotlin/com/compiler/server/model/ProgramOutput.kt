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
  val jvmByteCode: String? = null,
  val exception: Exception? = null,
  val restriction: String? = null
) {
  fun asExecutionResult(): JvmExecutionResult {
    return when {
      restriction != null -> JvmExecutionResult().apply { text = buildRestriction(restriction) }
      exception != null -> JvmExecutionResult(exception = exception.toExceptionDescriptor())
      standardOutput.isBlank() -> JvmExecutionResult()
      else -> {
        try {
          // coroutines can produce incorrect output. see example in `base coroutines test 7`
          if (standardOutput.startsWith("{")) outputMapper.readValue(standardOutput, JvmExecutionResult::class.java)
          else {
            val result = outputMapper.readValue("{" + standardOutput.substringAfter("{"), JvmExecutionResult::class.java)
            result.apply {
              text = standardOutput.substringBefore("{") + text
            }
          }
        } catch (e: Exception) {
          JvmExecutionResult(exception = e.toExceptionDescriptor())
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