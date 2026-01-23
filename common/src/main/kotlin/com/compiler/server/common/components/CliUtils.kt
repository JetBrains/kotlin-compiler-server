package com.compiler.server.common.components

import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
fun <T> usingTempDirectory(action: (path: Path) -> T): T {
  val path = getTempDirectory()
  path.createDirectories()
  return try {
    action(path)
  } finally {
    path.deleteRecursively()
  }
}

private fun getTempDirectory(): Path {
  val dir = System.getProperty("java.io.tmpdir")
  val sessionId = UUID.randomUUID().toString().replace("-", "")
  return File(dir).canonicalFile.resolve(sessionId).toPath()
}

val PATH_SEPARATOR: String = File.pathSeparator
