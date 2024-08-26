package indexation

import model.ImportInfo
import component.KotlinEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory

class JvmIndexationBuilder(private val kotlinEnvironment: KotlinEnvironment): IndexationBuilder() {
  override fun getAllIndexes(): List<ImportInfo> =
    kotlinEnvironment.environment { coreEnvironment ->
      val project = coreEnvironment.project
      val trace = CliBindingTrace(project)
      val componentProvider = TopDownAnalyzerFacadeForJVM.createContainer(
        project = project,
        files = emptyList(),
        trace = trace,
        configuration = coreEnvironment.configuration,
        packagePartProvider = { globalSearchScope ->
          coreEnvironment.createPackagePartProvider(globalSearchScope)
        },
        declarationProviderFactory = { storageManager, ktFiles ->
          FileBasedDeclarationProviderFactory(storageManager, ktFiles)
        }
      )
      val moduleDescriptor = componentProvider.getService(ModuleDescriptor::class.java)
      return@environment moduleDescriptor.allImportsInfo().distinct()
    }
}
