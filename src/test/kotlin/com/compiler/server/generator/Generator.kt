package com.compiler.server.generator

import com.compiler.server.compiler.model.Project
import com.compiler.server.compiler.model.ProjectFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


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

fun generateMultiProject(vararg files: String, args: String = ""): Project{
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

fun runManyTest(times: Int = 100, test: () -> Unit) {
  runBlocking {
    GlobalScope.launch(Dispatchers.IO) {
      for (i in 0 until times) {
        launch {
          test()
        }
      }
    }.join()
  }
}