package com.compiler.server.compiler.components

import com.compiler.server.model.*
import com.intellij.openapi.util.Pair
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import component.KotlinEnvironment
import model.Completion
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.js.klib.TopDownAnalyzerFacadeForJSIR
import org.jetbrains.kotlin.cli.js.klib.TopDownAnalyzerFacadeForWasmJs
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
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
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.FileScopeProviderImpl
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.wasm.resolve.WasmPlatformAnalyzerServices
import org.springframework.stereotype.Component

@Component
class ErrorAnalyzer(
  private val kotlinEnvironment: KotlinEnvironment,
  private val indexationProvider: IndexationProvider
) {
  fun errorsFrom(
    files: List<KtFile>,
    coreEnvironment: KotlinCoreEnvironment,
    projectType: ProjectType
  ): ErrorsAndAnalysis {
    val analysis = when {
        projectType.isJvmRelated() -> analysisOf(files, coreEnvironment)
        projectType.isJsRelated() -> analyzeFileForJs(files, coreEnvironment)
        projectType.isWasmRelated() -> analyzeFileForWasm(files, coreEnvironment)
        else -> throw IllegalArgumentException("Unknown platform: $projectType")
    }
    return ErrorsAndAnalysis(
      errorsFrom(
        analysis.analysisResult.bindingContext.diagnostics.all(),
        CompilerDiagnostics(files.associate { it.name to analyzeErrorsFrom(it, projectType) }),
        projectType
      ),
      analysis
    )
  }

  fun analysisOf(files: List<KtFile>, coreEnvironment: KotlinCoreEnvironment): Analysis {
    val trace = CliBindingTrace()
    val project = files.first().project
    val componentProvider = TopDownAnalyzerFacadeForJVM.createContainer(
      project = project,
      files = files,
      trace = trace,
      configuration = coreEnvironment.configuration,
      packagePartProvider = { globalSearchScope ->
        coreEnvironment.createPackagePartProvider(globalSearchScope)
      },
      declarationProviderFactory = { storageManager, ktFiles ->
        FileBasedDeclarationProviderFactory(storageManager, ktFiles)
      }
    )
    componentProvider.getService(LazyTopDownAnalyzer::class.java)
      .analyzeDeclarations(
        topDownAnalysisMode = TopDownAnalysisMode.TopLevelDeclarations,
        declarations = files
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
    return AnalysisJvm(
      componentProvider = componentProvider,
      analysisResult = AnalysisResult.success(trace.bindingContext, moduleDescriptor)
    )
  }

  fun analyzeFileForJs(files: List<KtFile>, coreEnvironment: KotlinCoreEnvironment): Analysis {
    val project = coreEnvironment.project
    val configuration = JsConfig(
      project,
      kotlinEnvironment.jsConfiguration,
      CompilerEnvironment,
      kotlinEnvironment.JS_METADATA_CACHE,
      kotlinEnvironment.JS_LIBRARIES.toSet()
    )

    val mainModule = MainModule.SourceFiles(files)
    val sourceModule = ModulesStructure(
      project,
      mainModule,
      kotlinEnvironment.jsConfiguration,
      kotlinEnvironment.JS_LIBRARIES,
      emptyList()
    )

    val mds = sourceModule.allDependencies.map {
      sourceModule.getModuleDescriptor(it) as ModuleDescriptorImpl
    }

    val builtInModuleDescriptor = sourceModule.builtInModuleDescriptor

    val analyzer = AnalyzerWithCompilerReport(kotlinEnvironment.jsConfiguration)
    val analyzerFacade = TopDownAnalyzerFacadeForJSIR
    val analysisResult = analyzerFacade.analyzeFiles(
      mainModule.files,
      project,
      kotlinEnvironment.jsConfiguration,
      mds,
      emptyList(),
      analyzer.targetEnvironment,
      thisIsBuiltInsModule = builtInModuleDescriptor == null,
      customBuiltInsModule = builtInModuleDescriptor
    )

    val context = ContextForNewModule(
      projectContext = ProjectContext(project, "COMPILER-SERVER-JS"),
      moduleName = Name.special("<" + configuration.moduleId + ">"),
      builtIns = JsPlatformAnalyzerServices.builtIns, platform = null
    )
    val dependencies = mutableSetOf(context.module) + mds + JsPlatformAnalyzerServices.builtIns.builtInsModule
    context.module.setDependencies(dependencies.toList())
    val trace = CliBindingTrace()
    val providerFactory = FileBasedDeclarationProviderFactory(context.storageManager, files)
    val analyzerAndProvider = createContainerForTopDownAnalyzerForJs(context, trace, providerFactory, JsPlatforms.defaultJsPlatform, JsPlatformAnalyzerServices)

    val hasErrors = analyzer.hasErrors()

    sourceModule.jsFrontEndResult = ModulesStructure.JsFrontEndResult(analysisResult, hasErrors)

    return AnalysisJs(
      sourceModule = sourceModule,
      componentProvider = analyzerAndProvider.second,
      analysisResult = analysisResult
    )
  }

  fun analyzeFileForWasm(files: List<KtFile>, coreEnvironment: KotlinCoreEnvironment): Analysis {
    val project = coreEnvironment.project
    val configuration = JsConfig(
      project,
      kotlinEnvironment.wasmConfiguration,
      CompilerEnvironment,
      emptyList(),
      kotlinEnvironment.WASM_LIBRARIES.toSet()
    )

    val mainModule = MainModule.SourceFiles(files)
    val sourceModule = ModulesStructure(
      project,
      mainModule,
      kotlinEnvironment.wasmConfiguration,
      kotlinEnvironment.WASM_LIBRARIES,
      emptyList()
    )

    val mds = sourceModule.allDependencies.map {
      sourceModule.getModuleDescriptor(it) as ModuleDescriptorImpl
    }

    val builtInModuleDescriptor = sourceModule.builtInModuleDescriptor

    val analyzer = AnalyzerWithCompilerReport(kotlinEnvironment.jsConfiguration)
    val analyzerFacade = TopDownAnalyzerFacadeForWasmJs
    val analysisResult = analyzerFacade.analyzeFiles(
      mainModule.files,
      project,
      kotlinEnvironment.wasmConfiguration,
      mds,
      emptyList(),
      analyzer.targetEnvironment,
      thisIsBuiltInsModule = builtInModuleDescriptor == null,
      customBuiltInsModule = builtInModuleDescriptor
    )

    val context = ContextForNewModule(
      projectContext = ProjectContext(project, "COMPILER-SERVER-JS"),
      moduleName = Name.special("<" + configuration.moduleId + ">"),
      builtIns = WasmPlatformAnalyzerServices.builtIns, platform = null
    )
    val dependencies = mutableSetOf(context.module) + mds + WasmPlatformAnalyzerServices.builtIns.builtInsModule
    context.module.setDependencies(dependencies.toList())
    val trace = CliBindingTrace()
    val providerFactory = FileBasedDeclarationProviderFactory(context.storageManager, files)
    val analyzerAndProvider = createContainerForTopDownAnalyzerForJs(context, trace, providerFactory, WasmPlatforms.Default, WasmPlatformAnalyzerServices)

    val hasErrors = analyzer.hasErrors()

    sourceModule.jsFrontEndResult = ModulesStructure.JsFrontEndResult(analysisResult, hasErrors)

    return AnalysisJs(
      sourceModule = sourceModule,
      componentProvider = analyzerAndProvider.second,
      analysisResult = analysisResult
    )
  }

  fun errorsFrom(
    diagnostics: Collection<Diagnostic>,
    compilerDiagnostics: CompilerDiagnostics,
    projectType: ProjectType
  ): CompilerDiagnostics = (compilerDiagnostics.map and errorsFrom(diagnostics, projectType)).map { (fileName, errors) ->
    fileName to errors.sortedWith(Comparator.comparing({ it.interval?.start }, nullsFirst()))
  }.toMap().let(::CompilerDiagnostics)

  private fun analyzeErrorsFrom(file: PsiFile, projectType: ProjectType): List<ErrorDescriptor> {
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
        interval = TextInterval.from(
          start = it.textRange.startOffset,
          end = it.textRange.endOffset,
          currentDocument = file.viewProvider.document!!
        ),
        message = it.errorDescription,
        severity = ProjectSeveriry.ERROR,
        className = "red_wavy_line",
        imports = completionsForErrorMessage(it.errorDescription, projectType)
      )
    }
  }

  private fun createContainerForTopDownAnalyzerForJs(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    declarationProviderFactory: DeclarationProviderFactory,
    platform: TargetPlatform,
    analyzerServices: PlatformDependentAnalyzerServices
  ): Pair<LazyTopDownAnalyzer, ComponentProvider> {
    val container = composeContainer(
      "TopDownAnalyzerForJs",
      analyzerServices.platformConfigurator.platformSpecificContainer
    ) {
      configureModule(
        moduleContext = moduleContext,
        platform = platform,
        analyzerServices = analyzerServices,
        trace = bindingTrace,
        languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
        optimizingOptions = null,
        absentDescriptorHandlerClass = null
      )
      useInstance(declarationProviderFactory)
      registerSingleton(AnnotationResolverImpl::class.java)
      registerSingleton(FileScopeProviderImpl::class.java)
      CompilerEnvironment.configure(this)
      useInstance(LookupTracker.DO_NOTHING)
      useInstance(InlineConstTracker.DoNothing)
      registerSingleton(ResolveSession::class.java)
      registerSingleton(LazyTopDownAnalyzer::class.java)
    }

    container.getService(ModuleDescriptorImpl::class.java)
      .initialize(container.getService(KotlinCodeAnalyzer::class.java).packageFragmentProvider)
    return Pair(container.getService(LazyTopDownAnalyzer::class.java), container)
  }

  private fun errorsFrom(
    diagnostics: Collection<Diagnostic>,
    projectType: ProjectType
  ) = diagnostics.mapNotNull { diagnostic ->
    diagnostic.psiFile.virtualFile?.let {
      val render = DefaultErrorMessages.render(diagnostic)
      if (!render.contains("This cast can never succeed")) {
        if (diagnostic.severity != Severity.INFO) {
          val textRanges = diagnostic.textRanges.iterator()
          if (textRanges.hasNext()) {
            var className = diagnostic.severity.name
            val imports = if (diagnostic.factory === Errors.UNRESOLVED_REFERENCE) {
              completionsForErrorMessage(render, projectType)
            } else null
            if (!(diagnostic.factory === Errors.UNRESOLVED_REFERENCE) && diagnostic.severity == Severity.ERROR) {
              className = "red_wavy_line"
            }
            val firstRange = textRanges.next()
            val interval = TextInterval.from(
              firstRange.startOffset,
              firstRange.endOffset,
              diagnostic.psiFile.viewProvider.document!!
            )
            diagnostic.psiFile.name to ErrorDescriptor(
              interval = interval,
              message = render,
              severity = ProjectSeveriry.from(diagnostic.severity),
              className = className,
              imports = imports
            )
          } else null
        } else null
      } else null
    }
  }.groupBy { it.first }.map { it.key to it.value.map { (_, error) -> error } }.toMap()

  private infix fun Map<String, List<ErrorDescriptor>>.and(errors: Map<String, List<ErrorDescriptor>>) =
    (this.toList() + errors.toList())
      .groupBy { it.first }
      .map { it.key to it.value.fold(emptyList<ErrorDescriptor>()) { acc, (_, errors) -> acc + errors } }
      .toMap()

  private fun completionsForErrorMessage(message: String, projectType: ProjectType): List<Completion>? {
    if (!indexationProvider.hasIndexes(projectType) ||
      !message.startsWith(IndexationProvider.UNRESOLVED_REFERENCE_PREFIX)
    ) return null
    val name = message.removePrefix(IndexationProvider.UNRESOLVED_REFERENCE_PREFIX)
    return indexationProvider.getClassesByName(name, projectType)?.map { suggest -> suggest.toCompletion() }
  }
}

data class ErrorsAndAnalysis(val compilerDiagnostics: CompilerDiagnostics, val analysis: Analysis)
