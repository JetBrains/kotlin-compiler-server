package com.compiler.server.generator

import com.compiler.server.model.Project
import com.compiler.server.model.ProjectFile
import com.compiler.server.model.ProjectType


fun generateSingleProject(text: String, args: String = "", projectType: ProjectType = ProjectType.JAVA): Project {
  val file = ProjectFile(
    text = text,
    name = "File.kt"
  )
  return Project(
    args = args,
    files = listOf(file),
    confType = projectType
  )
}

fun generateMultiProject(
  vararg files: String,
  args: String = "",
  projectType: ProjectType = ProjectType.JAVA
): Project {
  val projectFiles = files.mapIndexed { i, text ->
    ProjectFile(
      text = text,
      name = "File$i.kt"
    )
  }
  return Project(
    args = args,
    files = projectFiles,
    confType = projectType
  )
}
