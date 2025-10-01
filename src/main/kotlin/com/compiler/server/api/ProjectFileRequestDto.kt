package com.compiler.server.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectFileRequestDto(val text: String = "", val name: String = "")