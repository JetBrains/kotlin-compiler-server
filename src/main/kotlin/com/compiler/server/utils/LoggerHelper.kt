package com.compiler.server.utils

import com.compiler.server.model.ProjectType
import net.logstash.logback.marker.Markers
import org.slf4j.LoggerFactory

object LoggerHelper {
  private val log = LoggerFactory.getLogger(LoggerHelper::class.java)

  fun logUnsuccessfulExecutionResult(type: ProjectType, version: String) {
    log.info(
      Markers.appendFields(
        UnsuccessfulExecutionDetails(
          confType = type.toString(),
          version = version
        )
      ), "Execution is unsuccessful."
    )
  }

  private data class UnsuccessfulExecutionDetails(
    val confType: String,
    val version: String
  )
}