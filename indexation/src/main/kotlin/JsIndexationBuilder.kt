package indexation

import model.ImportInfo
import component.KotlinEnvironment
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.ir.backend.js.prepareAnalyzedSourceModule
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.resolve.CompilerEnvironment

class JsIndexationBuilder(private val kotlinEnvironment: KotlinEnvironment): IndexationBuilder() {
  override fun getAllIndexes(): List<ImportInfo> =
    kotlinEnvironment.environment { coreEnvironment ->
      val project = coreEnvironment.project

      val sourceModule = prepareAnalyzedSourceModule(
        project,
        coreEnvironment.getSourceFiles(),
        kotlinEnvironment.jsConfiguration,
        kotlinEnvironment.JS_LIBRARIES,
        friendDependencies = emptyList(),
        analyzer = AnalyzerWithCompilerReport(kotlinEnvironment.jsConfiguration),
      )

      val mds = sourceModule.allDependencies.map {
        sourceModule.getModuleDescriptor(it)
      }

      return@environment mds.flatMap { moduleDescriptor ->
        moduleDescriptor.allImportsInfo()
      }.distinct()
    }
}
