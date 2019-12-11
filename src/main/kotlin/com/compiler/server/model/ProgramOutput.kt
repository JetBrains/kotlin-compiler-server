package com.compiler.server.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper


data class ProgramOutput(
  val standardOutput: String = "",
  val exception: Exception? = null
) {
  fun asExecutionResult(): JavaExecutionResult = jacksonObjectMapper().readValue(standardOutput, JavaExecutionResult::class.java)

  fun asJUnitExecutionResult(): JunitExecutionResult {
    val result = jacksonObjectMapper().readValue(
      standardOutput.toString(),
      object : TypeReference<Map<String, List<TestDescription>>>() {}
    )
    return JunitExecutionResult(result)
  }
}