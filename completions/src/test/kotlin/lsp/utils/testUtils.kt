package lsp.utils

import org.eclipse.lsp4j.Position

const val CARET_MARKER = "<CARET>"

fun extractCaret(caretMarker: String = CARET_MARKER, code: () -> String): Pair<String, Position> {
    val lines = code().lines().toMutableList()
    var caretLine = -1
    var caretChar = -1

    for ((i, line) in lines.withIndex()) {
        val idx = line.indexOf(caretMarker)
        if (idx != -1) {
            caretLine = i
            caretChar = idx
            lines[i] = line.removeRange(idx, idx + caretMarker.length)
            break
        }
    }

    if (caretLine == -1) {
        throw IllegalArgumentException("No \"$caretMarker\" marker found in code")
    }

    return lines.joinToString("\n") to Position(caretLine, caretChar)
}
