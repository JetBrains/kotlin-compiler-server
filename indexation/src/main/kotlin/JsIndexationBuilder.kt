package indexation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import common.model.ImportInfo
import component.KotlinEnvironment
import org.jetbrains.kotlin.js.config.JsConfig
import java.io.File

class JsIndexationBuilder(private val kotlinEnvironment: KotlinEnvironment): IndexationBuilder {
  override fun createIndexes(outputFilename: String) {
    val imports = getAllVariants()
    File(outputFilename).writeText(jacksonObjectMapper().writeValueAsString(imports))
  }

  private fun getAllVariants(): List<ImportInfo> =
    kotlinEnvironment.environment { coreEnvironment ->
      val project = coreEnvironment.project
      val configuration = JsConfig(
        project,
        kotlinEnvironment.jsConfiguration,
        kotlinEnvironment.JS_METADATA_CACHE,
        kotlinEnvironment.JS_LIBRARIES.toSet()
      )

      return@environment configuration.moduleDescriptors.flatMap { moduleDescriptor ->
        moduleDescriptor.allImportsInfo()
      }.distinct()
    }
}
