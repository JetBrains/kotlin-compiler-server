package com.compiler.server.controllers

import com.compiler.server.compiler.components.KotlinProjectExecutor
import com.compiler.server.compiler.model.ErrorDescriptor
import com.compiler.server.compiler.model.Project
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RunRestController(private val kotlinProjectExecutor: KotlinProjectExecutor) {
  @PostMapping("/api/compiler/run")
  fun executeKotlinProjectEndpoint(@RequestBody project: Project) = kotlinProjectExecutor.run(project)

  @PostMapping("/api/compiler/translate")
  fun translateKotlinProjectEndpoint(@RequestBody project: Project) = kotlinProjectExecutor.convertToJs(project)

  @PostMapping("/api/compiler/complete")
  fun getKotlinCompleteEndpoint(
    @RequestBody project: Project,
    @RequestParam line: Int,
    @RequestParam ch: Int
  ) = kotlinProjectExecutor.complete(project, line, ch)

  @PostMapping("/api/compiler/highlight")
  fun highlightEndpoint(@RequestBody project: Project): Map<String, List<ErrorDescriptor>> = kotlinProjectExecutor.highlight(project)

}