package com.compiler.server.compiler.model

open class ExecutionResult(open val errors: Map<String, List<ErrorDescriptor>> = emptyMap())

data class JavaExecutionResult(
        val text: String,
        val exception: ExceptionDescriptor? = null,
        override val errors: Map<String, List<ErrorDescriptor>> = emptyMap()
) : ExecutionResult(errors)
