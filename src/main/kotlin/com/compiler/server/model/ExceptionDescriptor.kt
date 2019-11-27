package com.compiler.server.model

open class ExceptionDescriptor(
  val message: String,
  val fullName: String,
  val stackTrace: List<StackTraceElement> = emptyList(),
  val cause: ExceptionDescriptor? = null
)
