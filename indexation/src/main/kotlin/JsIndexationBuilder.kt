package indexation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import common.model.ImportInfo
import component.KotlinEnvironment
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import java.io.File

class JsIndexationBuilder(private val kotlinEnvironment: KotlinEnvironment): IndexationBuilder {
  override fun createIndexes(outputFilename: String) {
    val imports = getAllVariants()
    File(outputFilename).writeText(jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(imports))
  }

  private fun getAllVariants(): List<ImportInfo> {
    val imports = mutableListOf<ImportInfo>()
    kotlinEnvironment.environment { coreEnvironment ->
      val project = coreEnvironment.project
      val configuration = JsConfig(
        project,
        kotlinEnvironment.jsConfiguration,
        kotlinEnvironment.JS_METADATA_CACHE,
        kotlinEnvironment.JS_LIBRARIES.toSet()
      )

      configuration.moduleDescriptors.forEach { moduleDescriptor ->
        val packages = moduleDescriptor.allPackages()
        packages.forEach { fqName ->
          val packageViewDescriptor = moduleDescriptor.getPackage(fqName)
          val descriptors = packageViewDescriptor.memberScope
            .getContributedDescriptors(DescriptorKindFilter.ALL, MemberScope.ALL_NAME_FILTER)
          imports.addAll(descriptors.mapNotNull { it.toImportInfo() })
        }
      }
    }
    return imports.distinct()
  }
}