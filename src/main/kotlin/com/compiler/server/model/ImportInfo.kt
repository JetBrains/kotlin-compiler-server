package com.compiler.server.model

data class ImportInfo (val fullName: String, val className: String, val jarName: String) {
  override fun toString(): String {
    return "{\nfullName: $fullName,\nclassName: $className,\njarName: $jarName\n}"
  }
}
