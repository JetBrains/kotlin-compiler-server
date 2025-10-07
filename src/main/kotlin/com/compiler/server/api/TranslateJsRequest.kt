package com.compiler.server.api

import com.compiler.server.model.ProjectType
import com.compiler.server.validation.CompilerArgumentsConstraint
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TranslateJsRequest(
    val args: String = "",
    val files: List<ProjectFileRequestDto> = listOf(),
    @CompilerArgumentsConstraint(ProjectType.JS)
    val firstPhaseCompilerArguments: Map<String, Any> = emptyMap(),
    @CompilerArgumentsConstraint(ProjectType.JS)
    val secondPhaseCompilerArguments: Map<String, Any> = emptyMap()
)
