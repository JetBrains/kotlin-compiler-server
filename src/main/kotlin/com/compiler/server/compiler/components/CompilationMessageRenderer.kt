package com.compiler.server.compiler.components

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import java.io.File

/**
 * Renders compiler messages in the `fileUri:line:column:lineEnd:columnEnd message` format, so they can
 * be processed by [CompilationLogger].
 */
object CompilationMessageRenderer : CompilerMessageRenderer {
    override fun render(
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
    ): String = buildString {
        location?.apply {
            append(File(path).toPath().toUri())
            if (line > 0 && column > 0) {
                // lineEnd and columnEnd are equal to -1 in case of no end range
                if(lineEnd > 0 && columnEnd > 0){
                    append(":$line:$column:$lineEnd:$columnEnd")
                }else{
                    append(":$line:$column:$line:$column")
                }
            }
            append(' ')
        }
        append(message)
    }
}
