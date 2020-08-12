package com.compiler.server.model

import common.model.Completion

class HighlightResult (
  val errors: Map<String, List<ErrorDescriptor>> = emptyMap(),
  var importsSuggestions: Map<String, List<Completion>> = emptyMap()
) {
  fun addSuggestions(suggestions: Map<String, List<Completion>>) {
    importsSuggestions = suggestions
  }
}