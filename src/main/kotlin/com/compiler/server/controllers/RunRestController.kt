package com.compiler.server.controllers

import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.Project
import com.compiler.server.service.KotlinProjectExecutor
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@RestController
@RequestMapping("/api/compiler")
class RunRestController(private val kotlinProjectExecutor: KotlinProjectExecutor) {
  @PostMapping("/run")
  fun executeKotlinProjectEndpoint(@RequestBody project: Project) = kotlinProjectExecutor.run(project)

  @PostMapping("/run-streaming")
  fun executeKotlinProjectStreamingEndpoint(@RequestBody project: Project) = StreamingResponseBody { outputStream ->
    kotlinProjectExecutor.runStreaming(project, outputStream)
  }

  @PostMapping("/test")
  fun testKotlinProjectEndpoint(@RequestBody project: Project) = kotlinProjectExecutor.test(project)

  @PostMapping("/test-streaming")
  fun testKotlinProjectStreamingEndpoint(@RequestBody project: Project) = StreamingResponseBody { outputStream ->
    kotlinProjectExecutor.testStreaming(project, outputStream)
  }

  @PostMapping("/translate")
  fun translateKotlinProjectEndpoint(@RequestBody project: Project) = kotlinProjectExecutor.convertToJs(project)

  @PostMapping("/complete")
  fun getKotlinCompleteEndpoint(
    @RequestBody project: Project,
    @RequestParam line: Int,
    @RequestParam ch: Int
  ) = kotlinProjectExecutor.complete(project, line, ch)

  @PostMapping("/highlight")
  fun highlightEndpoint(@RequestBody project: Project): Map<String, List<ErrorDescriptor>> = kotlinProjectExecutor.highlight(project)

  @GetMapping("/version")
  fun getKotlinVersionEndpoint() = kotlinProjectExecutor.getVersion()

}