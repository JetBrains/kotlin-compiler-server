package com.compiler.server.api

import com.compiler.server.model.ProjectType
import com.compiler.server.validation.CompilerArgumentsConstraint
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class TranslateWasmRequest(
    val args: String = "",
    val files: List<ProjectFileRequestDto> = listOf(),
    @CompilerArgumentsConstraint(ProjectType.WASM)
    val firstPhaseCompilerArguments: Map<String, Any> = emptyMap(),
    @CompilerArgumentsConstraint(ProjectType.WASM)
    val secondPhaseCompilerArguments: Map<String, Any> = emptyMap()
)