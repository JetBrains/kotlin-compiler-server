package com.compiler.server.compiler.components

import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ProjectSeveriry
import com.compiler.server.model.TextInterval
import org.jetbrains.kotlin.buildtools.api.KotlinLogger

class CompilationLogger : KotlinLogger {
    override val isDebugEnabled: Boolean = false

    var warnings: Map<String, MutableList<ErrorDescriptor>> = emptyMap()

    override fun debug(msg: String) {

    }

    override fun error(msg: String, throwable: Throwable?) {
        try {
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
//                TextInterval(TextInterval.TextPosition(endLine, endCh), TextInterval.TextPosition(endLine, endCh)),
                message,
                ProjectSeveriry.ERROR,
                className
            )
            warnings["$className.kt"]?.add(ed)
        } catch (_: Exception) {
        }
    }

    override fun info(msg: String) {

    }

    override fun lifecycle(msg: String) {

    }

    override fun warn(msg: String, throwable: Throwable?) {
        try {
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
//                TextInterval(TextInterval.TextPosition(endLine, endCh), TextInterval.TextPosition(endLine, endCh)),
                message,
                ProjectSeveriry.WARNING,
                "WARNING"
            )
            warnings["$className.kt"]?.add(ed)
        } catch (_: Exception) {
        }
    }

}
