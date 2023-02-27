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

  fun hasErrors() =
    textWithError() || exception != null || errors.any { (_, value) -> value.any { it.severity == ProjectSeveriry.ERROR } }

  private fun textWithError() = text.startsWith(ERROR_STREAM_START)
}

abstract class TranslationResultWithJsCode(
  open val jsCode: String?,
  errors: Map<String, List<ErrorDescriptor>>,
  exception: ExceptionDescriptor?
) : ExecutionResult(errors, exception)

data class TranslationJSResult(
  override val jsCode: String? = null,
  override var exception: ExceptionDescriptor? = null,
  override var errors: Map<String, List<ErrorDescriptor>> = emptyMap()
) : TranslationResultWithJsCode(jsCode, errors, exception)

data class TranslationWasmResult(
  override val jsCode: String? = null,
  val jsInstantiated: String,
  val wasm: ByteArray,
  override var exception: ExceptionDescriptor? = null,
  override var errors: Map<String, List<ErrorDescriptor>> = emptyMap()
) : TranslationResultWithJsCode(jsCode, errors, exception)

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class JunitExecutionResult(
  val testResults: Map<String, List<TestDescription>> = emptyMap(),
  override var exception: ExceptionDescriptor? = null,
  override var errors: Map<String, List<ErrorDescriptor>> = emptyMap()
) : ExecutionResult(errors, exception)

private fun unEscapeOutput(value: String) = value.replace("&amp;lt;".toRegex(), "<")
  .replace("&amp;gt;".toRegex(), ">")
  .replace("\r", "")
