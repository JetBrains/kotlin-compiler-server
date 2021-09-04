package com.compiler.server.utils

import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.ProjectType
import net.logstash.logback.marker.Markers
import org.slf4j.LoggerFactory

object LoggerHelper {
  private val log = LoggerFactory.getLogger(LoggerHelper::class.java)

  fun logUnsuccessfulExecutionResult(executionResult: ExecutionResult, type: ProjectType, version: String) {
    if (executionResult.isUnsuccessful().not()) return
    log.info(
      Markers.appendFields(
        UnsuccessfulExecutionDetails(
          executionResult.errorsMessages(),
          type.toString(),
          version
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