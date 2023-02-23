package com.compiler.server.compiler.components

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.klib.generateIrForKlibSerialization
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.ic.CacheUpdater
import org.jetbrains.kotlin.ir.backend.js.ic.DirtyFileState
import org.jetbrains.kotlin.ir.backend.js.ic.ModuleArtifact
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

// copy from compiler private funs

fun processSourceModule(
  project: Project,
  files: List<KtFile>,
  libraries: List<String>,
  friendLibraries: List<String>,
  configuration: CompilerConfiguration,
  outputKlibPath: String
): ModulesStructure {
  val sourceModule: ModulesStructure = prepareAnalyzedSourceModule(
    project,
    files,
    configuration,
    libraries,
    friendLibraries,
    AnalyzerWithCompilerReport(configuration)
  )

  val moduleSourceFiles = (sourceModule.mainModule as MainModule.SourceFiles).files
  val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()

  val (moduleFragment, _) = generateIrForKlibSerialization(
    project,
    moduleSourceFiles,
    configuration,
    sourceModule.jsFrontEndResult.jsAnalysisResult,
    sourceModule.allDependencies.map { it.library },
    emptyList(),
    expectDescriptorToSymbol,
    IrFactoryImpl,
    verifySignatures = true
  ) {
    sourceModule.getModuleDescriptor(it)
  }

  val metadataSerializer =
    KlibMetadataIncrementalSerializer(
      configuration,
      sourceModule.project,
      sourceModule.jsFrontEndResult.hasErrors
    )

  generateKLib(
    sourceModule,
    outputKlibPath,
    nopack = true,
    jsOutputName = null,
    icData = emptyList(),
    expectDescriptorToSymbol = expectDescriptorToSymbol,
    moduleFragment = moduleFragment
  ) { file ->
    metadataSerializer.serializeScope(file, sourceModule.jsFrontEndResult.bindingContext, moduleFragment.descriptor)
  }
  return sourceModule
}

fun prepareIcCaches(
  includes: String,
  cacheDirectory: String,
  libraries: List<String>,
  friendLibraries: List<String>,
  mainCallArgs: List<String>,
  configurationJs: CompilerConfiguration,
): List<ModuleArtifact> {
  val cacheUpdater = CacheUpdater(
    mainModule = includes,
    allModules = libraries,
    mainModuleFriends = friendLibraries,
    cacheDir = cacheDirectory,
    compilerConfiguration = configurationJs,
    irFactory = { IrFactoryImplForJsIC(WholeWorldStageController()) },
    mainArguments = mainCallArgs,
    compilerInterfaceFactory = { mainModule, cfg ->
      JsIrCompilerWithIC(
        mainModule,
        cfg,
        JsGenerationGranularity.WHOLE_PROGRAM,
        es6mode = false
      )
    }
  )

  return cacheUpdater.actualizeCaches()
}