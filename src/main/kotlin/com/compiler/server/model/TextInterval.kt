package com.compiler.server.model


data class TextInterval(val start: TextPosition, val end: TextPosition) {
  data class TextPosition(val line: Int, val ch: Int) : Comparable<TextPosition> {
    override fun compareTo(other: TextPosition): Int = compareValuesBy(this, other, { it.line }, { it.ch })
  }

}
