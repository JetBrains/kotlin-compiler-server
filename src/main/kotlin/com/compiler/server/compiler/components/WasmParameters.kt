package com.compiler.server.compiler.components

data class WasmParameters(
    val dependencies: List<String>,
    val plugins: List<String>,
    val pluginOptions: List<String>,
)