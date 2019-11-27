package com.compiler.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ErrorDescriptor(
  val interval: TextInterval,
  val message: String,
  val severity: ProjectSeveriry,
  val className: String? = null
)
