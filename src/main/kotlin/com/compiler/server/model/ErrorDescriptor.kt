package com.compiler.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import model.Completion

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorDescriptor(
  val interval: TextInterval,
  val message: String,
  val severity: ProjectSeveriry,
  val className: String? = null,
  val imports: List<Completion>? = null
)
