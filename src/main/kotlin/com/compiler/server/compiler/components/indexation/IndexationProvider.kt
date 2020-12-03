package com.compiler.server.compiler.components.indexation

import com.compiler.server.service.KotlinProjectExecutor
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import common.model.ImportInfo
import org.apache.commons.logging.LogFactory
import java.io.File

open class IndexationProvider {
  companion object {
    const val UNRESOLVED_REFERENCE_PREFIX = "Unresolved reference: "
  }
  protected var allIndexes: List<ImportInfo>? = null
  protected val log = LogFactory.getLog(KotlinProjectExecutor::class.java)!!

  protected fun readIndexesFromJson(filename: String): List<ImportInfo> =
    jacksonObjectMapper().readValue(File(filename).readText())

  internal fun hasIndexes() = allIndexes != null

  internal fun getClassesByName(name: String) =
    allIndexes?.filter { it.shortName == name }
}