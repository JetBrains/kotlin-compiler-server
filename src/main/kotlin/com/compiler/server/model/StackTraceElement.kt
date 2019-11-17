package com.compiler.server.model

data class StackTraceElement(
        val className: String,
        val methodName: String,
        val fileName: String,
        val lineNumber: Int
)