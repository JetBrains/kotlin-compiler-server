package com.compiler.server.compiler.model

data class StackTraceElement(
        val className: String,
        val methodName: String,
        val fileName: String,
        val lineNumber: Int
)