package com.compiler.server.model.bean

data class VersionInfo(
  val version: String,
  val stdlibVersion: String,
  val latestStable: Boolean = true,
  val unstable: Boolean = false
)