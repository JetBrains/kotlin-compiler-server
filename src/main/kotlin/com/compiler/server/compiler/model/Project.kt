package com.compiler.server.compiler.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Project(
  val args: String = "",
  val files: List<ProjectFile> = listOf(),
  val confType: String = "java",
  val readOnlyFileNames: List<String> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectFile(
  val text: String = "",
  val name: String = "",
  val type: Type = Type.KOTLIN_FILE
)

enum class Type {
  KOTLIN_FILE,
  KOTLIN_TEST_FILE
}