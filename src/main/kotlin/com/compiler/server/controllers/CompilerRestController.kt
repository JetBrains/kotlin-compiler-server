package com.compiler.server.controllers

import com.compiler.server.model.*
import com.compiler.server.model.bean.VersionInfo
import com.compiler.server.service.KotlinProjectExecutor
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(value = ["/api/compiler", "/api/**/compiler"])
class CompilerRestController(private val kotlinProjectExecutor: KotlinProjectExecutor) {
  @PostMapping("/run")
  fun executeKotlinProjectEndpoint(
    @RequestBody project: Project,
    @RequestParam(defaultValue = "false") addByteCode: Boolean,
  ): ExecutionResult {
    return kotlinProjectExecutor.run(project, addByteCode)
  }

  @PostMapping("/test")
  fun testKotlinProjectEndpoint(
    @RequestBody project: Project,
    @RequestParam(defaultValue = "false") addByteCode: Boolean,
  ): ExecutionResult {
    return kotlinProjectExecutor.test(project, addByteCode)
  }

  @PostMapping("/translate")
  fun translateKotlinProjectEndpoint(
    @RequestBody project: Project,
    @RequestParam(defaultValue = "js") compiler: String,
    @RequestParam(defaultValue = "false") debugInfo: Boolean
  ): TranslationResultWithJsCode {
    return when (KotlinTranslatableCompiler.valueOf(compiler.uppercase().replace("-", "_"))) {
      KotlinTranslatableCompiler.JS -> kotlinProjectExecutor.convertToJsIr(project)
      KotlinTranslatableCompiler.WASM -> kotlinProjectExecutor.convertToWasm(
        project,
        debugInfo,
        multiModule = false,
      )
      KotlinTranslatableCompiler.COMPOSE_WASM -> kotlinProjectExecutor.convertToWasm(
        project,
        debugInfo,
        multiModule = true,
      )
    }
  }

  @PostMapping("/complete")
  fun getKotlinCompleteEndpoint(
    @RequestBody project: Project,
    @RequestParam line: Int,
    @RequestParam ch: Int
  ) = kotlinProjectExecutor.complete(project, line, ch)

  @PostMapping("/highlight")
  fun highlightEndpoint(@RequestBody project: Project): CompilerDiagnostics =
    kotlinProjectExecutor.highlight(project)
}

@RestController
class VersionRestController(private val kotlinProjectExecutor: KotlinProjectExecutor) {
  @GetMapping("/versions")
  fun getKotlinVersionEndpoint(): List<VersionInfo> = listOf(kotlinProjectExecutor.getVersion())
}
