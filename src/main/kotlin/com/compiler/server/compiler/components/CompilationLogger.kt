package com.compiler.server.compiler.components

import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ProjectSeveriry
import com.compiler.server.model.TextInterval
import org.jetbrains.kotlin.buildtools.api.KotlinLogger


/**
 * This custom implementation of Kotlin Logger is needed for sending compilation logs to the user
 * on the frontend instead of printing them on the stderr. CompilationLogger extracts data from logs
 * and saves it in [compilationLogs] map, so that compilation messages can be later displayed to
 * the user, and their position can be marked in their code.

 * KotlinLogger interface will be changed in the future to contain more log details.
 * Implementation of the CompilationLogger should be therefore updated after KT-80963 is implemented.
 *
 * @property isDebugEnabled A flag to indicate whether debug-level logging is enabled for the logger.
 *                          If true, all messages are printed to the standard output.
 */
class CompilationLogger(
    override val isDebugEnabled: Boolean = false,
) : KotlinLogger {

    /**
     * Stores a collection of compilation logs organized by file paths.
     *
     * The map keys represent file paths as strings, and the associated values are mutable lists of
     * `ErrorDescriptor` objects containing details about compilation issues, such as error messages,
     * intervals, severity, and optional class names.
     */
    var compilationLogs: Map<String, MutableList<ErrorDescriptor>> = emptyMap()

    override fun debug(msg: String) {
        if (isDebugEnabled) println("[DEBUG] $msg")
    }

    override fun error(msg: String, throwable: Throwable?) {
        if (isDebugEnabled) println("[ERROR] $msg" + (throwable?.let { ": ${it.message}" } ?: ""))
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
        if (isDebugEnabled) println("[WARN] $msg" + (throwable?.let { ": ${it.message}" } ?: ""))
        try {
            addCompilationLog(msg, ProjectSeveriry.WARNING, classNameOverride = "WARNING")
        } catch (_: Exception) {}
    }


    /**
     * Adds a compilation log entry to the `compilationLogs` map based on the string log.
     *
     * @param msg The raw log message containing information about the compilation event,
     *            including the file path and error details.
     * @param severity The severity level of the compilation event, represented by the `ProjectSeveriry` enum.
     * @param classNameOverride An optional override for the class name that will be recorded in the log.
     *                          If null, it will be derived from the file path in the message.
     */
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
