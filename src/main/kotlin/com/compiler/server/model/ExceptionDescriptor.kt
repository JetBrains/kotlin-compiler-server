package com.compiler.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
open class ExceptionDescriptor(
  val message: String? = null,
  val fullName: String? = null,
  val stackTrace: List<StackTraceElement> = emptyList(),
  val cause: ExceptionDescriptor? = null,
  val localizedMessage: String? = null
)
