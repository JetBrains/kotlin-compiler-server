package com.compiler.server.model

data class JsCompilerArguments(
    val firstPhase: Map<String, Any>,
    val secondPhase: Map<String, Any>
)