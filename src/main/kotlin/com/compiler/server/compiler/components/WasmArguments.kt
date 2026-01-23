package com.compiler.server.compiler.components

import com.compiler.server.model.ExtendedCompilerArgument
import java.io.Serializable

data class WasmArguments(
    val defaultCompilerArgs: Set<ExtendedCompilerArgument>,
    val firstPhasePredefinedArguments: Map<String, Serializable>,
    val secondPhasePredefinedArguments: Map<String, Serializable>,
    val outputFileName: String,
)