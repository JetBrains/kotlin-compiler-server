package com.compiler.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class StackTraceElement(
  val className: String = "",
  val methodName: String = "",
  val fileName: String = "",
  val lineNumber: Int = 0
)