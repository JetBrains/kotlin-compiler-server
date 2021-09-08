package com.compiler.server.utils

import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.ProjectType
import net.logstash.logback.marker.Markers
import org.slf4j.LoggerFactory

object LoggerHelper {
  private val log = LoggerFactory.getLogger(LoggerHelper::class.java)

  fun logUnsuccessfulExecutionResult(executionResult: ExecutionResult, type: ProjectType, version: String) {
    log.info(
      Markers.appendFields(
        UnsuccessfulExecutionDetails(
          hasErrors = executionResult.hasErrors(),
          confType = type.toString(),
          version = version
        )
      ), "Execution is unsuccessful."
    )
  }

  private data class UnsuccessfulExecutionDetails(
    val hasErrors: Boolean,
    val confType: String,
    val version: String
  )
}