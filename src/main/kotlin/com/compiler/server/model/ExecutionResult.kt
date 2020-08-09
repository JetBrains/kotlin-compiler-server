package com.compiler.server.model

import com.fasterxml.jackson.annotation.JsonInclude

open class ExecutionResult(
  open var errors: Map<String, List<ErrorDescriptor>> = emptyMap(),
  open var exception: ExceptionDescriptor? = null
) {
  var text: String = ""
    set(value) {
      field = unEscapeOutput(value)
    }
  var importsSuggestions: Map<String, List<Completion>> = emptyMap()

  fun addSuggestions(suggestions: Map<String, List<Completion>>) {
    importsSuggestions = suggestions
  }

  fun addWarnings(warnings: Map<String, List<ErrorDescriptor>>) {
    errors = warnings
  }
}

data class TranslationJSResult(
  val jsCode: String? = null,
  override var exception: ExceptionDescriptor? = null,
  override var errors: Map<String, List<ErrorDescriptor>> = emptyMap()
) : ExecutionResult(errors, exception)

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class JunitExecutionResult(
  val testResults: Map<String, List<TestDescription>> = emptyMap(),
  override var exception: ExceptionDescriptor? = null,
  override var errors: Map<String, List<ErrorDescriptor>> = emptyMap()
) : ExecutionResult(errors, exception)

private fun unEscapeOutput(value: String) = value.replace("&amp;lt;".toRegex(), "<")
  .replace("&amp;gt;".toRegex(), ">")
  .replace("\r", "")
