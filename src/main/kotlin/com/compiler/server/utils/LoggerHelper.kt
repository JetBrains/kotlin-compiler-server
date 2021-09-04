package com.compiler.server.utils

import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.ProjectType
import net.logstash.logback.marker.Markers
import org.slf4j.LoggerFactory

object LoggerHelper {
  private val log = LoggerFactory.getLogger(LoggerHelper::class.java)

  fun logUnsuccessfulExecutionResult(executionResult: ExecutionResult, type: ProjectType, version: String) {
    val errors = executionResult.getErrorMessages()
    if (errors.isEmpty()) return
    log.info(
      Markers.appendFields(
        UnsuccessfulExecutionDetails(
          executionErrors = errors,
          confType = type.toString(),
          version = version
        )
      ), "Execution is unsuccessful."
    )
  }

  private data class UnsuccessfulExecutionDetails(
    val executionErrors: List<String>,
    val confType: String,
    val version: String
  )
}