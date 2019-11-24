package com.compiler.server.controllers

import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.Project
import com.compiler.server.service.KotlinProjectExecutor
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/compiler")
class RunRestController(private val kotlinProjectExecutor: KotlinProjectExecutor) {
  @PostMapping("/run")
  fun executeKotlinProjectEndpoint(@RequestBody project: Project) = kotlinProjectExecutor.run(project)

  @PostMapping("/test")
  fun testKotlinProjectEndpoint(@RequestBody project: Project) = kotlinProjectExecutor.test(project)

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