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

data class ProgramOutput(
  val standardOutput: String = "",
  val exception: Exception? = null,
  val restriction: String? = null
) {
  fun asExecutionResult(): ExecutionResult {
    return when {
      restriction != null -> ExecutionResult().apply { text = buildRestriction(restriction) }
      else -> {
        // coroutines can produced incorrect output. see example in `base coroutines test 7`
        if (standardOutput.startsWith("{")) outputMapper.readValue(standardOutput, ExecutionResult::class.java)
        else {
          val result = outputMapper.readValue("{" + standardOutput.substringAfter("{"), ExecutionResult::class.java)
          result.apply {
            text = standardOutput.substringBefore("{") + text
          }
        }
      }
    }
  }

  fun asJUnitExecutionResult(): JunitExecutionResult {
    return when {
      restriction != null -> JunitExecutionResult().apply { text = buildRestriction(restriction) }
      else -> {
        val result = outputMapper.readValue(
          standardOutput,
          object : TypeReference<Map<String, List<TestDescription>>>() {}
        )
        JunitExecutionResult(result)
      }
    }
  }

  private fun buildRestriction(restriction: String) = "<errStream>$restriction</errStream>"
}