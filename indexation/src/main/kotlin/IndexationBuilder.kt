package indexation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import model.ImportInfo
import java.io.File

abstract class IndexationBuilder {
  fun writeIndexesToFile(outputFilename: String) {
    val indexes = getAllIndexes().groupBy { it.shortName }
    File(outputFilename).writeText(jacksonObjectMapper().writeValueAsString(indexes))
  }

  abstract fun getAllIndexes(): List<ImportInfo>
}
