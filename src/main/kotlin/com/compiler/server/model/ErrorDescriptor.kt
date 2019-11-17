package com.compiler.server.model

data class ErrorDescriptor(
  val interval: TextInterval,
  val message: String,
  val severity: ProjectSeveriry,
  val className: String? = null
)
