package com.compiler.server.model

class HighlightResult (
  val errors: Map<String, List<ErrorDescriptor>> = emptyMap(),
  var importsSuggestions: Map<String, CompletionData> = emptyMap()
) {
  fun addSuggestions(suggestions: Map<String, CompletionData>) {
    importsSuggestions = suggestions
  }
}