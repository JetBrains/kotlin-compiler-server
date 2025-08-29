package com.compiler.server.api

import com.compiler.server.model.ProjectType
import com.compiler.server.validation.CompilerArgumentsConstraint
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TestRequest(
    val args: String = "",
    val files: List<ProjectFileRequestDto> = listOf(),
    @CompilerArgumentsConstraint(ProjectType.JAVA)
    val compilerArguments: Map<String, Any> = emptyMap()
)
