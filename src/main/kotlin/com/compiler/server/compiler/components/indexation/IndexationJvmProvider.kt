package com.compiler.server.compiler.components.indexation

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class IndexationJvmProvider(
  @Value("\${indexes.file}") private val indexesFileName: String
): IndexationProvider() {
  @PostConstruct
  private fun initIndexes() {
    allIndexes = kotlin.runCatching { readIndexesFromJson(indexesFileName) }.getOrNull()
    if (allIndexes == null) {
      log.warn("Server started without auto imports.")
    }
  }
}