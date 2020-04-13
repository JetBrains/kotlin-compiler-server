package com.compiler.server.controllers

import com.compiler.server.model.Project
import com.compiler.server.model.ProjectType
import com.compiler.server.service.KotlinProjectExecutor
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@RestController
@ConditionalOnProperty(name = ["enable.streaming.controller"], havingValue = "true")
class KotlinPlaygroundStreamingRestController(private val kotlinProjectExecutor: KotlinProjectExecutor) {

  /**
   * An endpoint for kotlin playground requests with streaming output.
   * Kotlin Playground component: https://github.com/JetBrains/kotlin-playground
   */
  @RequestMapping(
    value = ["/kotlinServer"],
    method = [RequestMethod.POST],
    consumes = [MediaType.ALL_VALUE],
    headers = ["Enable-Streaming=true"]
  )
  fun tryKotlinLangObsoleteEndpointStreaming(
    @RequestParam type: String,
    @RequestParam(required = false) project: Project?
  ): ResponseEntity<StreamingResponseBody> {
    return when (type) {
      "run" -> {
        if (project == null) throw error("No parameter 'project' found")

        // those headers are needed to prevent browsers from buffering output chunks
        val responseBuilder = ResponseEntity.ok()
          .contentType(MediaType.TEXT_HTML)
          .header("X-Content-Type-Options", "nosniff")

        when (project.confType) {
          ProjectType.JAVA -> {
            responseBuilder.body(StreamingResponseBody { outputStream ->
              kotlinProjectExecutor.runStreaming(project, outputStream)
            })
          }
          ProjectType.JUNIT -> {
            responseBuilder.body(StreamingResponseBody { outputStream ->
              kotlinProjectExecutor.testStreaming(project, outputStream)
            })
          }
          else -> throw error("Streaming is only supported for 'java' and 'junit' configurations")
        }
      }
      else -> throw error("Streaming is only supported for 'run' requests")
    }
  }
}