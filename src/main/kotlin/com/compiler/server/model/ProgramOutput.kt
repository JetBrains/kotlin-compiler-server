package com.compiler.server.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper


data class ProgramOutput(
  val standardOutput: String = "",
  val exception: Exception? = null,
  val restriction: String? = null
) {
  fun asExecutionResult(): JavaExecutionResult {
    return when {
      restriction != null -> JavaExecutionResult().apply { text = restriction }
      else -> {
        // coroutines can produced incorrect output. see example in `base coroutines test 7`
        if (standardOutput.startsWith("{")) jacksonObjectMapper().readValue(standardOutput, JavaExecutionResult::class.java)
        else {
          val result = jacksonObjectMapper().readValue( "{" + standardOutput.substringAfter("{"), JavaExecutionResult::class.java)
          result.apply {
            text = standardOutput.substringBefore("{") + text
          }
        }
      }
    }
  }

  fun asJUnitExecutionResult(): JunitExecutionResult {
    return when {
      restriction != null -> JunitExecutionResult().apply { text = restriction }
      else -> {
        val result = jacksonObjectMapper().readValue(
          standardOutput,
          object : TypeReference<Map<String, List<TestDescription>>>() {}
        )
        JunitExecutionResult(result)
      }
    }
  }
}