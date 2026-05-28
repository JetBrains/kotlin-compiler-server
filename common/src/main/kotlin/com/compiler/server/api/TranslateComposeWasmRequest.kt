package com.compiler.server.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class TranslateComposeWasmRequest(
    val args: String = "",
    val files: List<ProjectFileRequestDto> = listOf(),
    val firstPhaseCompilerArguments: Map<String, Any> = emptyMap(),
    val secondPhaseCompilerArguments: Map<String, Any> = emptyMap(),
) : CacheableRequest
