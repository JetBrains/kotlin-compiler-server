package com.compiler.server.controllers

import com.compiler.server.api.CompilerArgumentResponse
import com.compiler.server.api.RunRequest
import com.compiler.server.api.TestRequest
import com.compiler.server.api.TranslateComposeWasmRequest
import com.compiler.server.api.TranslateJsRequest
import com.compiler.server.api.TranslateWasmRequest
import com.compiler.server.model.CompilerDiagnostics
import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.KotlinTranslatableCompiler
import com.compiler.server.model.Project
import com.compiler.server.model.ProjectFile
import com.compiler.server.model.ProjectType
import com.compiler.server.model.TranslationResultWithJsCode
import com.compiler.server.service.CompilerArgumentsService
import com.compiler.server.service.KotlinProjectExecutor
import jakarta.validation.Valid
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/compiler", "/api/**/compiler"])
class CompilerRestController(
    private val kotlinProjectExecutor: KotlinProjectExecutor,
    private val compilerArgumentsService: CompilerArgumentsService
) {

    @PostMapping("/run")
    fun executeKotlinProjectEndpoint(
        @RequestBody @Valid request: RunRequest,
        @RequestParam(defaultValue = "false") addByteCode: Boolean,
    ): ExecutionResult {
        return kotlinProjectExecutor.run(
            Project(
                args = request.args,
                files = request.files.map { ProjectFile(name = it.name, text = it.text) },
                compilerArguments = listOf(request.compilerArguments)
            ), addByteCode
        )
    }

    @PostMapping("/test")
    fun testKotlinProjectEndpoint(
        @RequestBody @Valid request: TestRequest,
        @RequestParam(defaultValue = "false") addByteCode: Boolean,
    ): ExecutionResult {
        return kotlinProjectExecutor.test(
            Project(
                args = request.args,
                files = request.files.map { ProjectFile(name = it.name, text = it.text) },
                compilerArguments = listOf(request.compilerArguments)
            ), addByteCode
        )
    }

    @PostMapping("/translate/js")
    fun translateJs(@RequestBody @Valid request: TranslateJsRequest): TranslationResultWithJsCode {
        return kotlinProjectExecutor.convertToJsIr(
            Project(
                args = request.args,
                files = request.files.map { ProjectFile(name = it.name, text = it.text) },
                compilerArguments = listOf(request.firstPhaseCompilerArguments, request.secondPhaseCompilerArguments)
            )
        )
    }

    @PostMapping("/translate/wasm")
    fun translateWasm(
        @RequestBody @Valid request: TranslateWasmRequest,
    ): TranslationResultWithJsCode {
        return kotlinProjectExecutor.convertToWasm(
            Project(
                args = request.args,
                files = request.files.map { ProjectFile(name = it.name, text = it.text) },
                confType = ProjectType.WASM,
                compilerArguments = listOf(request.firstPhaseCompilerArguments, request.secondPhaseCompilerArguments))
        )
    }

    @PostMapping("/translate/compose-wasm")
    fun translateWasmCompose(
        @RequestBody @Valid request: TranslateComposeWasmRequest,
    ): TranslationResultWithJsCode {
        return kotlinProjectExecutor.convertToWasm(
            Project(
                args = request.args,
                files = request.files.map { ProjectFile(name = it.name, text = it.text) },
                confType = ProjectType.COMPOSE_WASM,
                compilerArguments = listOf(request.firstPhaseCompilerArguments, request.secondPhaseCompilerArguments))
        )
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


    @GetMapping("/compiler-arguments")
    fun getCompilerArguments(
        @RequestParam projectType: ProjectType,
    ): CompilerArgumentResponse =
        CompilerArgumentResponse(
            compilerArgumentsService.getCompilerArguments(projectType)
                .mapToSetOrEmpty {
                    CompilerArgumentResponse.CompilerArgument(
                        it.name,
                        it.shortName,
                        it.description,
                        it.type,
                        it.disabled,
                        it.predefinedValues
                    )
                }
        )

    @PostMapping("/translate")
    @Deprecated("Use /translate/wasm or /translate/js instead")
    fun translate(
        @RequestBody @Valid project: Project,
        @RequestParam(defaultValue = "js") compiler: String,
        @RequestParam(defaultValue = "false") debugInfo: Boolean
    ): TranslationResultWithJsCode {
        return when (KotlinTranslatableCompiler.valueOf(compiler.uppercase().replace("-", "_"))) {
            KotlinTranslatableCompiler.JS -> kotlinProjectExecutor.convertToJsIr(project)
            KotlinTranslatableCompiler.WASM -> kotlinProjectExecutor.convertToWasm(project, debugInfo)
            KotlinTranslatableCompiler.COMPOSE_WASM -> kotlinProjectExecutor.convertToWasm(project, debugInfo)
        }
    }
}
