package indexation

import common.model.ImportInfo
import component.KotlinEnvironment
import org.jetbrains.kotlin.js.config.JsConfig

class JsIndexationBuilder(private val kotlinEnvironment: KotlinEnvironment): IndexationBuilder() {
  override fun getAllIndexes(): List<ImportInfo> =
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
