package com.compiler.server.controllers

import com.compiler.server.model.Project
import com.compiler.server.model.ProjectType
import com.compiler.server.service.KotlinProjectExecutor
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@RestController
class KotlinPlaygroundRestController(private val kotlinProjectExecutor: KotlinProjectExecutor) {

  /**
   * Endpoint for support requests from kotlin playground client.
   * Kotlin Playground component see: https://github.com/JetBrains/kotlin-playground
   * Old server see: https://github.com/JetBrains/kotlin-web-demo
   */
  @RequestMapping(
    value = ["/kotlinServer"],
    method = [RequestMethod.GET, RequestMethod.POST],
    consumes = [MediaType.ALL_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
    headers = ["Enable-Streaming!=true"]
  )
  @Suppress("IMPLICIT_CAST_TO_ANY")
  fun tryKotlinLangObsoleteEndpoint(
    @RequestParam type: String,
    @RequestParam(required = false) line: Int?,
    @RequestParam(required = false) ch: Int?,
    @RequestParam(required = false) project: Project?
  ): ResponseEntity<*> {
    val result = when (type) {
      "getKotlinVersions" -> listOf(kotlinProjectExecutor.getVersion())
      else -> {
        if (project == null) throw error("No parameter 'project' found")
        when (type) {
          "run" -> {
            when (project.confType) {
              ProjectType.JAVA -> kotlinProjectExecutor.run(project)
              ProjectType.JS, ProjectType.CANVAS -> kotlinProjectExecutor.convertToJs(project)
              ProjectType.JUNIT -> kotlinProjectExecutor.test(project)
            }
          }
          "highlight" -> kotlinProjectExecutor.highlight(project)
          "complete" -> {
            if (line != null && ch != null) {
              kotlinProjectExecutor.complete(project, line, ch)
            }
            else throw error("No parameters 'line' or 'ch'")
          }
          else -> throw error("No parameter 'type' found")
        }
      }
    }
    return ResponseEntity.ok(result)
  }

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
  @Suppress("IMPLICIT_CAST_TO_ANY")
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