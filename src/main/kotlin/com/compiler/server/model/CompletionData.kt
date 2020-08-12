package com.compiler.server.model

import common.model.Completion

data class CompletionData (
  val intervals: List<TextInterval>,
  val imports: List<Completion>
)