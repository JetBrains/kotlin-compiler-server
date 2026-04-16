package com.compiler.server.cacheproxy.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectRequest(
    val args: String = "",
    val files: List<FileDto> = listOf(),
    val compilerArguments: List<Map<String, Any>> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ComposeWasmV2Request(
    val args: String = "",
    val files: List<FileDto> = listOf(),
    val firstPhaseCompilerArguments: Map<String, Any> = emptyMap(),
    val secondPhaseCompilerArguments: Map<String, Any> = emptyMap(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FileDto(val text: String = "", val name: String = "")
