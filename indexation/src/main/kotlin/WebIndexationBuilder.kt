package indexation

import com.compiler.server.common.components.KotlinEnvironment
import model.ImportInfo
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.prepareAnalyzedSourceModule
import org.jetbrains.kotlin.library.impl.isKotlinLibrary
import java.io.File

class WebIndexationBuilder(
  private val kotlinEnvironment: KotlinEnvironment,
  inputConfiguration: CompilerConfiguration,
  private val libraries: List<String>,
  private val compilerPlugins: List<String>,
  private val compilerPluginOptions: List<String>,
  private val platformConfiguration: CompilerConfiguration
): IndexationBuilder() {

  private val configuration = inputConfiguration.copy()

  override fun getAllIndexes(): List<ImportInfo> =
    kotlinEnvironment.environment { coreEnvironment ->
      val project = coreEnvironment.project

      if (compilerPlugins.isNotEmpty()) {
        PluginCliParser.loadPluginsSafe(
          compilerPlugins,
          compilerPluginOptions,
          emptyList(),
          configuration,
          kotlinEnvironment.disposable
        )
      }
      val sourceModule = prepareAnalyzedSourceModule(
        project,
        coreEnvironment.getSourceFiles(),
        configuration,
        libraries.filter { isKotlinLibrary(File(it)) },
        friendDependencies = emptyList(),
        analyzer = AnalyzerWithCompilerReport(platformConfiguration),
      )

      val mds = sourceModule.allDependencies.map {
        sourceModule.getModuleDescriptor(it)
      }

      return@environment mds.flatMap { moduleDescriptor ->
        moduleDescriptor.allImportsInfo()
      }.distinct()
    }
}