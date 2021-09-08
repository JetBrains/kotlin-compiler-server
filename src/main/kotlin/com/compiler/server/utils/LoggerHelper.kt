package com.compiler.server.utils

import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.ProjectType
import net.logstash.logback.marker.Markers
import org.slf4j.LoggerFactory

object LoggerHelper {
  private val log = LoggerFactory.getLogger(LoggerHelper::class.java)

  fun logExecutionResult(executionResult: ExecutionResult, type: ProjectType, version: String) {
    log.info(
      Markers.appendFields(
        ExecutionDetails(
          hasErrors = executionResult.hasErrors(),
          confType = type.toString(),
          version = version
        )
      ), "Code execution is complete."
    )
  }

  private data class ExecutionDetails(
    val hasErrors: Boolean,
    val confType: String,
    val version: String
  )
}