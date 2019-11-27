package com.compiler.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestDescription(
  var output: String = "",
  var className: String = "",
  var methodName: String = "",
  var executionTime: Long = 0,
  val exception: ExceptionDescriptor? = null,
  val comparisonFailure: ComparisonFailureDescriptor? = null,
  val status: TestStatus = TestStatus.OK
)

enum class TestStatus {
  OK,
  FAIL,
  ERROR
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ComparisonFailureDescriptor(
  message: String = "",
  fullName: String = "",
  stackTrace: List<StackTraceElement> = emptyList(),
  cause: ExceptionDescriptor? = null,
  val expected: String = "",
  val actual: String = ""
) : ExceptionDescriptor(message, fullName, stackTrace, cause)