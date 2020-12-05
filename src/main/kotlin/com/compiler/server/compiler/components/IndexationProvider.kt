package com.compiler.server.compiler.components

import com.compiler.server.service.KotlinProjectExecutor
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import common.model.ImportInfo
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File

@Component
class IndexationProvider(
  @Value("\${indexes.file}") private val indexesFileName: String,
  @Value("\${indexesJs.file}") private val indexesJsFileName: String
) {
  companion object {
    const val UNRESOLVED_REFERENCE_PREFIX = "Unresolved reference: "
  }

  internal fun hasIndexes(isJs: Boolean) =
    if (!isJs) {
      jvmIndexes != null
    } else {
      jsIndexes != null
    }

  internal fun getClassesByName(name: String, isJs: Boolean) =
    if (!isJs) {
      jvmIndexes?.filter { it.shortName == name }
    } else {
      jsIndexes?.filter { it.shortName == name }
    }

  private val jvmIndexes: List<ImportInfo>? by lazy {
    initIndexes(indexesFileName)
  }

  private val jsIndexes: List<ImportInfo>? by lazy {
    initIndexes(indexesJsFileName)
  }

  private val log = LogFactory.getLog(KotlinProjectExecutor::class.java)

  private fun readIndexesFromJson(fileName: String): List<ImportInfo> =
    jacksonObjectMapper().readValue(File(fileName).readText())

  private fun initIndexes(fileName: String): List<ImportInfo>? {
    val indexes = kotlin.runCatching { readIndexesFromJson(fileName) }.getOrNull()
    if (indexes == null) {
      log.warn("Server started without auto jvm imports.")
    }
    return indexes
  }
}