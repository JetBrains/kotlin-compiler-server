package com.compiler.server.model

open class ExecutionResult(open var errors: Map<String, List<ErrorDescriptor>> = emptyMap()) {
  fun addWarnings(warnings: Map<String, List<ErrorDescriptor>>) {
    errors = warnings
  }
}

data class JavaExecutionResult(
  val text: String = "",
  val exception: ExceptionDescriptor? = null,
  override var errors: Map<String, List<ErrorDescriptor>> = emptyMap()
) : ExecutionResult(errors)

data class TranslationJSResult(
  val jsCode: String? = null,
  override var errors: Map<String, List<ErrorDescriptor>> = emptyMap()
) : ExecutionResult(errors)

class JunitExecutionResult(val testResults: Map<String, List<TestDescription>>) : ExecutionResult()