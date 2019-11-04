package com.compiler.server.compiler.model

enum class Severity {
    INFO,
    ERROR,
    WARNING;
    companion object {
        fun from(severity: org.jetbrains.kotlin.diagnostics.Severity): Severity {
            return when (severity) {
                org.jetbrains.kotlin.diagnostics.Severity.ERROR -> ERROR
                org.jetbrains.kotlin.diagnostics.Severity.INFO -> INFO
                org.jetbrains.kotlin.diagnostics.Severity.WARNING -> WARNING
                else -> WARNING
            }
        }
    }
}