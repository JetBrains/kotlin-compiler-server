package com.compiler.server.compiler.model

data class ExceptionDescriptor(
        val message: String,
        val fullName: String,
        val stackTrace: List<StackTraceElement> = emptyList(),
        val cause: ExceptionDescriptor? = null
)
