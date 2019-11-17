package com.compiler.server.controllers

import com.compiler.server.compiler.components.KotlinProjectExecutor
import com.compiler.server.compiler.model.ErrorDescriptor
import com.compiler.server.compiler.model.Project
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/compiler")
class RunRestController(private val kotlinProjectExecutor: KotlinProjectExecutor) {
  @PostMapping("/run")
  fun executeKotlinProjectEndpoint(@RequestBody project: Project) = kotlinProjectExecutor.run(project)

  @PostMapping("/translate")
  fun translateKotlinProjectEndpoint(@RequestBody project: Project) = kotlinProjectExecutor.convertToJs(project)

  @PostMapping("/complete")
  fun getKotlinCompleteEndpoint(
    @RequestBody project: Project,
    @RequestParam(defaultValue = "java") runConf: String,
    @RequestParam line: Int,
    @RequestParam ch: Int
  ) = kotlinProjectExecutor.complete(project, line, ch, isJs = runConf == "js")

  @PostMapping("/highlight")
  fun highlightEndpoint(@RequestBody project: Project): Map<String, List<ErrorDescriptor>> = kotlinProjectExecutor.highlight(project)

}