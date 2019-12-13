package com.compiler.server.model

import com.fasterxml.jackson.annotation.JsonInclude

open class ExecutionResult(open var errors: Map<String, List<ErrorDescriptor>> = emptyMap()) {
  fun addWarnings(warnings: Map<String, List<ErrorDescriptor>>) {
    errors = warnings
  }
}

data class JavaExecutionResult(
  val exception: ExceptionDescriptor? = null,
  override var errors: Map<String, List<ErrorDescriptor>> = emptyMap()
) : ExecutionResult(errors) {
  var text: String = ""
    set(value) {
      field = unEscapeOutput(value)
    }
}

data class TranslationJSResult(
  val jsCode: String? = null,
  override var errors: Map<String, List<ErrorDescriptor>> = emptyMap()
) : ExecutionResult(errors)

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class JunitExecutionResult(
  val testResults: Map<String, List<TestDescription>> = emptyMap(),
  override var errors: Map<String, List<ErrorDescriptor>> = emptyMap()
) : ExecutionResult() {
  var text: String = ""
    set(value) {
      field = unEscapeOutput(value)
    }
}

private fun unEscapeOutput(value: String) = value.replace("&amp;lt;".toRegex(), "<")
  .replace("&amp;gt;".toRegex(), ">")
  .replace("\r", "")
