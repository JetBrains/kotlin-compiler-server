package com.compiler.server.generator

import com.compiler.server.compiler.model.Project
import com.compiler.server.compiler.model.ProjectFile


fun generateSingleProject(text: String, args: String = ""): Project {
  val file = ProjectFile(
    text = text,
    name = "File.kt"
  )
  return Project(
    args = args,
    files = listOf(file)
  )
}

fun generateMultiProject(vararg files: String, args: String = ""): Project {
  val projectFiles = files.mapIndexed { i, text ->
    ProjectFile(
      text = text,
      name = "File$i.kt"
    )
  }
  return Project(
    args = args,
    files = projectFiles
  )
}
