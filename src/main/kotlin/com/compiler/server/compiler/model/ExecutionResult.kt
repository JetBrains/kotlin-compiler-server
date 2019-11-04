package com.compiler.server.compiler.model

import com.compiler.server.compiler.JavaExecutor

open class ExecutionResult(open val errors: Map<String, List<ErrorDescriptor>> = emptyMap())

data class JavaExecutionResult(val text: String, val exception: JavaExecutor.ExceptionDescriptor? = null, override val errors: Map<String, List<ErrorDescriptor>> = emptyMap()) : ExecutionResult(errors)
