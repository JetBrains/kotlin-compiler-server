package com.compiler.server.model

open class ExecutionResult(open val errors: Map<String, List<ErrorDescriptor>> = emptyMap())

data class JavaExecutionResult(
  val text: String = "",
  val exception: ExceptionDescriptor? = null,
  override val errors: Map<String, List<ErrorDescriptor>> = emptyMap()
) : ExecutionResult(errors)

data class TranslationJSResult(
  val jsCode: String? = null,
  override val errors: Map<String, List<ErrorDescriptor>> = emptyMap()
) : ExecutionResult(errors)

class JunitExecutionResult(val testResults: Map<String, List<TestDescription>>) : ExecutionResult()