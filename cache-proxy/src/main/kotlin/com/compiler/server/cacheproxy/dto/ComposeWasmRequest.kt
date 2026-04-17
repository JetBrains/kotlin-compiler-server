package com.compiler.server.cacheproxy.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

sealed interface ComposeWasmRequest {
    val args: String
    val files: List<FileDto>
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectRequest(
    override val args: String = "",
    override val files: List<FileDto> = listOf(),
    val compilerArguments: List<Map<String, Any>> = emptyList(),
) : ComposeWasmRequest

@JsonIgnoreProperties(ignoreUnknown = true)
data class ComposeWasmV2Request(
    override val args: String = "",
    override val files: List<FileDto> = listOf(),
    val firstPhaseCompilerArguments: Map<String, Any> = emptyMap(),
    val secondPhaseCompilerArguments: Map<String, Any> = emptyMap(),
) : ComposeWasmRequest

@JsonIgnoreProperties(ignoreUnknown = true)
data class FileDto(val text: String = "", val name: String = "")
