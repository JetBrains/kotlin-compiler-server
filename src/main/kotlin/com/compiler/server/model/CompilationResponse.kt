package com.compiler.server.model

import com.fasterxml.jackson.annotation.JsonProperty

data class CompilationResponse(
    val success: Boolean,
    @field:JsonProperty("errors")
    val errors: CompilerDiagnostics
)

