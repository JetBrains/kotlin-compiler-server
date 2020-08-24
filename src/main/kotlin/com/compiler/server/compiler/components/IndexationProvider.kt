package com.compiler.server.compiler.components

import com.compiler.server.model.ErrorDescriptor
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
class IndexationProvider {
  @Value("\${indexes.file}") private val indexesFileName: String = ""

  private var ALL_INDEXES: List<ImportInfo>? = null
  private val log = LogFactory.getLog(KotlinProjectExecutor::class.java)
  private val UNRESOLVED_REFERENCE_PREFIX = "Unresolved reference: "

  @PostConstruct
  private fun initIndexes() {
    println("INIT")
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

  internal fun addImportsForErrorDescriptors(errors: List<ErrorDescriptor>): List<ErrorDescriptor> {
    return if (ALL_INDEXES == null) {
      errors
    } else errors.map {
      if (!it.message.startsWith(UNRESOLVED_REFERENCE_PREFIX)) return@map it
      val name = it.message.removePrefix(UNRESOLVED_REFERENCE_PREFIX)
      val suggestions = getClassesByName(name)?.map { suggest -> suggest.toCompletion() } ?: return@map it
      ErrorDescriptor(
        interval = it.interval,
        message = it.message,
        severity = it.severity,
        className = it.className,
        imports = suggestions
      )
    }
  }
}