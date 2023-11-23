package com.compiler.server.model

import com.intellij.openapi.editor.Document

data class TextInterval(val start: TextPosition, val end: TextPosition) {
  data class TextPosition(val line: Int, val ch: Int)
  companion object {
    fun from(start: Int, end: Int, currentDocument: Document): TextInterval {
      val lineNumberForElementStart = currentDocument.getLineNumber(start)
      val lineNumberForElementEnd = currentDocument.getLineNumber(end)
      var charNumberForElementStart = start - currentDocument.getLineStartOffset(lineNumberForElementStart)
      var charNumberForElementEnd = end - currentDocument.getLineStartOffset(lineNumberForElementStart)
      if ((start == end) && (lineNumberForElementStart == lineNumberForElementEnd)) {
        charNumberForElementStart--
        if (charNumberForElementStart < 0) {
          charNumberForElementStart++
          charNumberForElementEnd++
        }
      }
      return TextInterval(
        TextPosition(lineNumberForElementStart, charNumberForElementStart),
        TextPosition(lineNumberForElementEnd, charNumberForElementEnd)
      )
    }
  }
}