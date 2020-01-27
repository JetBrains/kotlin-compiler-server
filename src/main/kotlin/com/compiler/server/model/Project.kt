package com.compiler.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue

@JsonIgnoreProperties(ignoreUnknown = true)
data class Project(
  val args: String = "",
  val files: List<ProjectFile> = listOf(),
  val confType: ProjectType = ProjectType.JAVA,
  val bundleType: BundleType = BundleType.NONE
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectFile(val text: String = "", val name: String = "")

enum class ProjectType(@JsonValue val id: String) {
  JAVA("java"),
  JUNIT("junit"),
  CANVAS("canvas"),
  JS("js")
}

enum class BundleType(@JsonValue val id: String) {
  NONE("none"),
  PLAIN("plain"),
  MINIMIZED("minimized")
}
