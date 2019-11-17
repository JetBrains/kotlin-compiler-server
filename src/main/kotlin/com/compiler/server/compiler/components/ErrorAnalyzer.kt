package com.compiler.server.compiler.components

import com.compiler.server.model.Analysis
import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ProjectSeveriry
import com.compiler.server.model.TextInterval
import com.intellij.openapi.util.Pair
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.FileScopeProviderImpl
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.springframework.stereotype.Component
import java.io.File
import java.util.ArrayList
import kotlin.Comparator

@Component
class ErrorAnalyzer(private val kotlinEnvironment: KotlinEnvironment) {

  val jsConfiguration: CompilerConfiguration = kotlinEnvironment.coreEnvironment.configuration.copy().apply {
    put(CommonConfigurationKeys.MODULE_NAME, "moduleId")
    put(JSConfigurationKeys.LIBRARIES, listOf(File("js").absolutePath))
  }

  fun errorsFrom(files: List<KtFile>, isJs: Boolean = false): Map<String, List<ErrorDescriptor>> {
    val analysis = if (isJs.not()) analysisOf(files) else analyzeFileForJs(files)
    return errorsFrom(
      analysis.analysisResult.bindingContext.diagnostics.all(),
      files.map { it.name to anylizeErrorsFrom(it) }.toMap()
    )
  }

  fun analysisOf(files: List<KtFile>): Analysis = CliBindingTrace().let { trace ->
    val project = files.first().project
    val componentProvider = TopDownAnalyzerFacadeForJVM.createContainer(
      project = project,
      files = files,
      trace = trace,
      configuration = kotlinEnvironment.coreEnvironment.configuration,
      packagePartProvider = { globalSearchScope ->
        kotlinEnvironment.coreEnvironment.createPackagePartProvider(globalSearchScope)
      },
      declarationProviderFactory = { storageManager, ktFiles ->
        FileBasedDeclarationProviderFactory(storageManager, ktFiles)
      }
    )
    componentProvider.getService(LazyTopDownAnalyzer::class.java)
      .analyzeDeclarations(
        topDownAnalysisMode = TopDownAnalysisMode.TopLevelDeclarations,
        declarations = files,
        outerDataFlowInfo = DataFlowInfo.EMPTY
      )
    val moduleDescriptor = componentProvider.getService(ModuleDescriptor::class.java)
    AnalysisHandlerExtension.getInstances(project)
      .find {
        it.analysisCompleted(
          project = project,
          module = moduleDescriptor,
          bindingTrace = trace,
          files = files
        ) != null
      }
    Analysis(
      componentProvider = componentProvider,
      analysisResult = AnalysisResult.success(trace.bindingContext, moduleDescriptor)
    )
  }

  fun analyzeFileForJs(files: List<KtFile>): Analysis {
    val project = kotlinEnvironment.coreEnvironment.project
    val configuration = JsConfig(project, jsConfiguration)
    val module = ContextForNewModule(
      projectContext = ProjectContext(project, "COMPILER-SERVER-JS"),
      moduleName = Name.special("<" + configuration.moduleId + ">"),
      builtIns = JsPlatformAnalyzerServices.builtIns, platform = null
    )
    module.setDependencies(computeDependencies(module.module, configuration))
    val trace = CliBindingTrace()
    val providerFactory = FileBasedDeclarationProviderFactory(module.storageManager, files)
    val analyzerAndProvider = createContainerForTopDownAnalyzerForJs(module, trace, providerFactory)
    return Analysis(
      componentProvider = analyzerAndProvider.second,
      analysisResult = TopDownAnalyzerFacadeForJS.analyzeFiles(files, configuration)
    )
  }

  fun errorsFrom(
    diagnostics: Collection<Diagnostic>,
    errors: Map<String, List<ErrorDescriptor>>
  ): Map<String, List<ErrorDescriptor>> {
    return (errors and errorsFrom(diagnostics)).map { (fileName, errors) ->
      fileName to errors.sortedWith(Comparator { o1, o2 ->
        val line = o1.interval.start.line.compareTo(o2.interval.start.line)
        when (line) {
          0 -> o1.interval.start.ch.compareTo(o2.interval.start.ch)
          else -> line
        }
      })
    }.toMap()
  }

  fun isOnlyWarnings(errors: Map<String, List<ErrorDescriptor>>) = errors.none { it.value.any { error -> error.severity == ProjectSeveriry.ERROR } }

  private fun anylizeErrorsFrom(file: PsiFile): List<ErrorDescriptor> {
    class Visitor : PsiElementVisitor() {
      val errors = mutableListOf<PsiErrorElement>()
      override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
      }

      override fun visitErrorElement(element: PsiErrorElement) {
        errors.add(element)
      }
    }
    return Visitor().apply { visitFile(file) }.errors.map {
      ErrorDescriptor(
        TextInterval.from(
          start = it.textRange.startOffset,
          end = it.textRange.endOffset,
          currentDocument = file.viewProvider.document!!
        ), it.errorDescription, ProjectSeveriry.ERROR, "red_wavy_line"
      )
    }
  }

  private fun computeDependencies(module: ModuleDescriptorImpl, config: JsConfig): List<ModuleDescriptorImpl> {
    val allDependencies = ArrayList<ModuleDescriptorImpl>()
    allDependencies.add(module)
    config.moduleDescriptors.mapTo(allDependencies) { it }
    allDependencies.add(JsPlatformAnalyzerServices.builtIns.builtInsModule)
    return allDependencies
  }

  private fun createContainerForTopDownAnalyzerForJs(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    declarationProviderFactory: DeclarationProviderFactory
  ): Pair<LazyTopDownAnalyzer, ComponentProvider> {
    val container = composeContainer(
      "TopDownAnalyzerForJs",
      JsPlatformAnalyzerServices.platformConfigurator.platformSpecificContainer
    ) {
      configureModule(
        moduleContext = moduleContext,
        platform = JsPlatforms.defaultJsPlatform,
        analyzerServices = JsPlatformAnalyzerServices,
        trace = bindingTrace,
        languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
      )
      useInstance(declarationProviderFactory)
      registerSingleton(AnnotationResolverImpl::class.java)
      registerSingleton(FileScopeProviderImpl::class.java)
      CompilerEnvironment.configure(this)
      useInstance(LookupTracker.DO_NOTHING)
      registerSingleton(ResolveSession::class.java)
      registerSingleton(LazyTopDownAnalyzer::class.java)
    }

    container.getService(ModuleDescriptorImpl::class.java)
      .initialize(container.getService(KotlinCodeAnalyzer::class.java).packageFragmentProvider)
    return Pair(container.getService(LazyTopDownAnalyzer::class.java), container)
  }

  private fun errorsFrom(diagnostics: Collection<Diagnostic>) = diagnostics.mapNotNull { diagnostic ->
    diagnostic.psiFile.virtualFile?.let {
      val render = DefaultErrorMessages.render(diagnostic)
      if (!render.contains("This cast can never succeed")) {
        if (diagnostic.severity != Severity.INFO) {
          val textRanges = diagnostic.textRanges.iterator()
          if (textRanges.hasNext()) {
            var className = diagnostic.severity.name
            if (!(diagnostic.factory === Errors.UNRESOLVED_REFERENCE) && diagnostic.severity == Severity.ERROR) {
              className = "red_wavy_line"
            }
            val firstRange = textRanges.next()
            val interval = TextInterval.from(firstRange.startOffset, firstRange.endOffset, diagnostic.psiFile.viewProvider.document!!)
            diagnostic.psiFile.name to ErrorDescriptor(interval, render, ProjectSeveriry.from(diagnostic.severity), className)
          }
          else null
        }
        else null
      }
      else null
    }
  }.groupBy { it.first }.map { it.key to it.value.map { (_, error) -> error } }.toMap()

  private infix fun Map<String, List<ErrorDescriptor>>.and(errors: Map<String, List<ErrorDescriptor>>) =
    (this.toList() + errors.toList())
      .groupBy { it.first }
      .map { it.key to it.value.fold(emptyList<ErrorDescriptor>()) { acc, (_, errors) -> acc + errors } }
      .toMap()
}