package com.compiler.server.compiler.components

import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ProjectSeveriry
import com.compiler.server.model.TextInterval
import org.jetbrains.kotlin.buildtools.api.KotlinLogger

class CompilationLogger : KotlinLogger{
    override val isDebugEnabled: Boolean = false

    var warnings : Map<String, List<ErrorDescriptor>> = emptyMap()

    override fun debug(msg: String) {

    }

    override fun error(msg: String, throwable: Throwable?) {
        val path = msg.split(" ")[0]
        val className = path.split("/").last().split(".").first()
        val message = msg.split(path)[1].drop(1)
        val splitPath = path.split(":")
        val line = splitPath[splitPath.size - 2].toInt() -1
        val ch = splitPath[splitPath.size - 1].toInt() -1
        val ed = ErrorDescriptor(TextInterval(TextInterval.TextPosition(line, ch), TextInterval.TextPosition(line, ch)), message, ProjectSeveriry.ERROR, className)
        warnings = warnings + (path to (warnings[path] ?: emptyList()) + ed)
    }

    override fun info(msg: String) {

    }

    override fun lifecycle(msg: String) {

    }

    override fun warn(msg: String, throwable: Throwable?) {
        println(msg)
        val path = msg.split(" ")[0]
        val className = path.split("/").last().split(".").first()
        val message = msg.split(path)[1].drop(1)
        val splitPath = path.split(":")
        val line = splitPath[splitPath.size - 2].toInt() -1
        val ch = splitPath[splitPath.size - 1].toInt() -1
        val ed = ErrorDescriptor(TextInterval(TextInterval.TextPosition(line, ch), TextInterval.TextPosition(line, ch)), message, ProjectSeveriry.WARNING, "WARNING")
//        val ed = ErrorDescriptor(TextInterval(TextInterval.TextPosition(line, ch), TextInterval.TextPosition(line, ch)), message, ProjectSeveriry.WARNING, className)
        warnings = warnings + (path to (warnings[path] ?: emptyList()) + ed)
    }

}
