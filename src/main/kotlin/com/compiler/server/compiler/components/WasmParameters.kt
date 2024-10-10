package com.compiler.server.compiler.components

import java.io.File

data class WasmParameters(
    val dependencies: List<String>,
    val plugins: List<String>,
    val pluginOptions: List<String>,
    val cacheDir: File?
)