package com.compiler.server.base

import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.ProjectSeveriry

val ExecutionResult.hasErrors: Boolean
  get() = compilerDiagnostics.hasErrors

val List<ErrorDescriptor>.hasErrors: Boolean
  get() = any { it.severity == ProjectSeveriry.ERROR }


val List<ErrorDescriptor>.filterOnlyErrors: List<ErrorDescriptor>
  get() = filter { it.severity == ProjectSeveriry.ERROR }


fun renderErrorDescriptors(errors: List<ErrorDescriptor>): String {
  return buildString {
    errors.forEach { appendLine(it.renderErrorDescriptor()) }
  }
}

fun ErrorDescriptor.renderErrorDescriptor(): String =
  interval?.let { "(${it.start.line}, ${it.end.line}): $message" } ?: message
