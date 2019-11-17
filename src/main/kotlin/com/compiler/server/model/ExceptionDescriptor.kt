package com.compiler.server.model

data class ExceptionDescriptor(
        val message: String,
        val fullName: String,
        val stackTrace: List<StackTraceElement> = emptyList(),
        val cause: ExceptionDescriptor? = null
)
