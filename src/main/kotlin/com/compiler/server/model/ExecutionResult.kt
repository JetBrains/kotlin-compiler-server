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

  fun addWarnings(warnings: Map<String, List<ErrorDescriptor>>) {
    errors = warnings
  }

  fun isUnsuccessful() =
    textWithError() || errors.any { (_, value) -> value.any { it.severity == ProjectSeveriry.ERROR } }

  fun errorsMessages(): List<String> {
    val compilationErrors =
      errors.flatMap { it.value }.filter { it.severity == ProjectSeveriry.ERROR }.map { it.message }

    return if (textWithError()) compilationErrors + text
    else compilationErrors
  }

  private fun textWithError() = text.startsWith(ERROR_STREAM_START)
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
