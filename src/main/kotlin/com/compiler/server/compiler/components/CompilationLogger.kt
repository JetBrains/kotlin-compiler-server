package com.compiler.server.compiler.components

import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ProjectSeveriry
import com.compiler.server.model.TextInterval
import org.jetbrains.kotlin.buildtools.api.KotlinLogger

class CompilationLogger(
    override val isDebugEnabled: Boolean = false,
) : KotlinLogger {

    var compilationLogs: Map<String, MutableList<ErrorDescriptor>> = emptyMap()

    override fun debug(msg: String) {
        if (isDebugEnabled) println("[DEBUG] $msg")
    }

    override fun error(msg: String, throwable: Throwable?) {
        if (isDebugEnabled) System.err.println("[ERROR] $msg" + (throwable?.let { ": ${it.message}" } ?: ""))
        try {
            addCompilationLog(msg, ProjectSeveriry.ERROR, classNameOverride = null)
        } catch (_: Exception) {}
    }

    override fun info(msg: String) {
        if (isDebugEnabled) println("[INFO] $msg")
    }

    override fun lifecycle(msg: String) {
        if (isDebugEnabled) println("[LIFECYCLE] $msg")
    }

    override fun warn(msg: String, throwable: Throwable?) {
        if (isDebugEnabled) System.err.println("[WARN] $msg" + (throwable?.let { ": ${it.message}" } ?: ""))
        try {
            addCompilationLog(msg, ProjectSeveriry.WARNING, classNameOverride = "WARNING")
        } catch (_: Exception) {}
    }

    private fun addCompilationLog(msg: String, severity: ProjectSeveriry, classNameOverride: String?) {
        val path = msg.split(" ")[0]
        val className = path.split("/").last().split(".").first()
        val message = msg.split(path)[1].drop(1)
        val splitPath = path.split(":")
        val line = splitPath[splitPath.size - 4].toInt() - 1
        val ch = splitPath[splitPath.size - 3].toInt() - 1
        val endLine = splitPath[splitPath.size - 2].toInt() - 1
        val endCh = splitPath[splitPath.size - 1].toInt() - 1
        val ed = ErrorDescriptor(
            TextInterval(TextInterval.TextPosition(line, ch), TextInterval.TextPosition(endLine, endCh)),
            message,
            severity,
            classNameOverride ?: className
        )
        compilationLogs["$className.kt"]?.add(ed)
    }
}
