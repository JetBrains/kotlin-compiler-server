package com.compiler.server.compiler.components

import com.compiler.server.service.KotlinProjectExecutor
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import common.model.ImportInfo
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import javax.annotation.PostConstruct

@Component
class IndexationProvider(
  @Value("\${indexes.file}") private val indexesFileName: String
) {
  companion object {
    val UNRESOLVED_REFERENCE_PREFIX = "Unresolved reference: "
  }
  private var ALL_INDEXES: List<ImportInfo>? = null
  private val log = LogFactory.getLog(KotlinProjectExecutor::class.java)

  @PostConstruct
  private fun initIndexes() {
    ALL_INDEXES = kotlin.runCatching { readIndexesFromJson() }.getOrNull()
    if (ALL_INDEXES == null) {
      log.warn("Server started without auto imports.")
    }
  }

  private fun readIndexesFromJson(): List<ImportInfo> =
    jacksonObjectMapper().readValue(File(indexesFileName).readText())

  internal fun hasIndexes() = ALL_INDEXES != null

  internal fun getClassesByName(name: String) =
    ALL_INDEXES?.filter { it.shortName == name }
}