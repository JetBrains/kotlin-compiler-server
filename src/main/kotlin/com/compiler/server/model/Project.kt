package com.compiler.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Project(
  val args: String = "",
  val files: List<ProjectFile> = listOf(),
  val confType: ProjectType = ProjectType.JAVA,
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

enum class ProjectType {
  JAVA,
  JUNIT,
  CANVAS,
  JS
}