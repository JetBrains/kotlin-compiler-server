package com.compiler.server.compiler.model

data class ErrorDescriptor(
  val interval: TextInterval,
  val message: String,
  val severity: ProjectSeveriry,
  val className: String? = null
)
