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
    produces = [MediaType.APPLICATION_JSON_VALUE]
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
        if (project == null) error("No parameter 'project' found")
        when (type) {
          "run" -> {
            when (project.confType) {
              ProjectType.JAVA -> kotlinProjectExecutor.run(project)
              ProjectType.JS, ProjectType.CANVAS -> kotlinProjectExecutor.convertToJs(project)
              ProjectType.JS_IR -> kotlinProjectExecutor.convertToJsIr(project)
              ProjectType.WASM -> kotlinProjectExecutor.convertToWasm(project)
              ProjectType.JUNIT -> kotlinProjectExecutor.test(project)
            }
          }

          "highlight" -> kotlinProjectExecutor.highlight(project)
          "complete" -> {
            if (line != null && ch != null) {
              kotlinProjectExecutor.complete(project, line, ch)
            } else error("No parameters 'line' or 'ch'")
          }

          else -> error("No parameter 'type' found")
        }
      }
    }
    return ResponseEntity.ok(result)
  }
}