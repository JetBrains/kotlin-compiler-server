package indexation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import common.model.ImportInfo
import java.io.File

abstract class IndexationBuilder {
  fun writeIndexesToFile(outputFilename: String) {
      File(outputFilename).writeText(jacksonObjectMapper().writeValueAsString(getAllIndexes()))
  }
  abstract fun getAllIndexes(): List<ImportInfo>
}
