package com.compiler.server.model

data class VersionInfo(
    val version: String,
    val stdlibVersion: String,
    val latestStable: Boolean = true
)