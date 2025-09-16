//package indexation
//
//import model.ImportInfo
//import component.KotlinEnvironment
//import org.jetbrains.kotlin.backend.common.LoadedKlibs
//import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
//import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
//import org.jetbrains.kotlin.config.CompilerConfiguration
//import org.jetbrains.kotlin.ir.backend.js.prepareAnalyzedSourceModule
//import org.jetbrains.kotlin.library.loader.KlibLoader
//
//class WebIndexationBuilder(
//  private val kotlinEnvironment: KotlinEnvironment,
//  inputConfiguration: CompilerConfiguration,
//  private val libraries: List<String>,
//  private val compilerPlugins: List<String>,
//  private val compilerPluginOptions: List<String>,
//  private val platformConfiguration: CompilerConfiguration
//): IndexationBuilder() {
//
//  private val configuration = inputConfiguration.copy()
//
//  override fun getAllIndexes(): List<ImportInfo> =
//    kotlinEnvironment.environment { coreEnvironment ->
//      val project = coreEnvironment.project
//
//      if (compilerPlugins.isNotEmpty()) {
//        PluginCliParser.loadPluginsSafe(
//          compilerPlugins,
//          compilerPluginOptions,
//          emptyList<String>(),
//          configuration,
//          kotlinEnvironment.rootDisposable
//        )
//      }
//
//      val klibs = LoadedKlibs(
//        all = KlibLoader {
//          libraryPaths(libraries)
//        }.load().librariesStdlibFirst
//      )
//
//      val sourceModule = prepareAnalyzedSourceModule(
//        project,
//        coreEnvironment.getSourceFiles(),
//        configuration,
//        klibs,
//        analyzer = AnalyzerWithCompilerReport(platformConfiguration),
//      )
//
//      val mds = sourceModule.klibs.all.map {
//        sourceModule.getModuleDescriptor(it)
//      }
//
//      return@environment mds.flatMap { moduleDescriptor ->
//        moduleDescriptor.allImportsInfo()
//      }.distinct()
//    }
//}