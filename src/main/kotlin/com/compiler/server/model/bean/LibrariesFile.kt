package com.compiler.server.model.bean

import java.io.File

class LibrariesFile(
  val jvm: File,
  val js: File,
  val wasm: File,
  val composeWasm: File,
  val composeWasmComposeCompiler: File
)