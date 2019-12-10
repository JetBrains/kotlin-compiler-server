package com.compiler.server.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class ProgramOutput(
  val standardOutput: String = "",
  val errorOutput: String = "",
  val exception: Exception? = null
) {
  fun asExecutionResult(): JavaExecutionResult {
    val errorText= if (errorOutput.isEmpty()) errorOutput else "<errStream>$errorOutput</errStream>"
    return JavaExecutionResult(
      text = "<outStream>$standardOutput\n</outStream>$errorText",
      exception = exception?.let {
        ExceptionDescriptor(it.message ?: "no message", it::class.java.toString())
      })
  }

  fun asJUnitExecutionResult(): JunitExecutionResult {
    val result = jacksonObjectMapper().readValue(
      standardOutput.toString(),
      object : TypeReference<Map<String, List<TestDescription>>>() {}
    )
    return JunitExecutionResult(result)
  }
}