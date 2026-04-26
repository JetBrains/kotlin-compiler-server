package com.compiler.server.model

import com.compiler.server.api.CacheableRequest
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue

@JsonIgnoreProperties(ignoreUnknown = true)
data class Project(
  val args: String = "",
  val files: List<ProjectFile> = listOf(),
  val confType: ProjectType = ProjectType.JAVA,
  val compilerArguments: List<Map<String, Any>> = emptyList()
) : CacheableRequest

@JsonIgnoreProperties(ignoreUnknown = true)
data class CompilerArgument(val name: String = "", val value: String = "")

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectFile(val text: String = "", val name: String = "")

enum class ProjectType(@JsonValue val id: String) {
  JAVA("java"),
  JUNIT("junit"),
  JS("js"),
  CANVAS("canvas"),
  JS_IR("js-ir"),
  WASM("wasm"),
  COMPOSE_WASM("compose-wasm");

}