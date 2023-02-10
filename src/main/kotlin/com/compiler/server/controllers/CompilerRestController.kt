package com.compiler.server.controllers

import com.compiler.server.model.*
import com.compiler.server.model.bean.VersionInfo
import com.compiler.server.service.KotlinProjectExecutor
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(value = ["/api/compiler", "/api/**/compiler"])
class CompilerRestController(private val kotlinProjectExecutor: KotlinProjectExecutor) {
  @PostMapping("/run")
  fun executeKotlinProjectEndpoint(@RequestBody project: Project): ExecutionResult {
    return kotlinProjectExecutor.run(project)
  }

  @PostMapping("/test")
  fun testKotlinProjectEndpoint(@RequestBody project: Project): ExecutionResult {
    return kotlinProjectExecutor.test(project)
  }

  @PostMapping("/translate")
  fun translateKotlinProjectEndpoint(
    @RequestBody project: Project,
    @RequestParam(defaultValue = "false") ir: Boolean,
    @RequestParam(defaultValue = "js") compiler: String
  ): TranslationResultWithJsCode {
    if (!ir) {
      return kotlinProjectExecutor.convertToJs(project)
    }
    return when (KotlinTranslatableCompiler.valueOf(compiler.uppercase())) {
      KotlinTranslatableCompiler.JS -> kotlinProjectExecutor.convertToJsIr(project)
      KotlinTranslatableCompiler.WASM -> kotlinProjectExecutor.convertToWasm(project)
    }
  }

  @PostMapping("/complete")
  fun getKotlinCompleteEndpoint(
    @RequestBody project: Project,
    @RequestParam line: Int,
    @RequestParam ch: Int
  ) = kotlinProjectExecutor.complete(project, line, ch)

  @PostMapping("/highlight")
  fun highlightEndpoint(@RequestBody project: Project): Map<String, List<ErrorDescriptor>> =
    kotlinProjectExecutor.highlight(project)
}

@RestController
class VersionRestController(private val kotlinProjectExecutor: KotlinProjectExecutor) {
  @GetMapping("/versions")
  fun getKotlinVersionEndpoint(): List<VersionInfo> = listOf(kotlinProjectExecutor.getVersion())
}