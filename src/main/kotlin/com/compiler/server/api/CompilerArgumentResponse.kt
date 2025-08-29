package com.compiler.server.api

import com.compiler.server.model.ExtendedCompilerArgumentValue
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class CompilerArgumentResponse(val compilerArguments: Set<CompilerArgument>) {

    data class CompilerArgument(
        val name: String,
        val shortName: String?,
        val description: String?,
        @field:JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
        val type: ExtendedCompilerArgumentValue<*>,
        val disabled: Boolean,
        val predefinedValues: Any?
    )
}

