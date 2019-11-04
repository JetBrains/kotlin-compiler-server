package com.compiler.server.compiler

import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

class KotlinCompiler(val environment: KotlinEnvironment) {

    open class Compiled(val files: Map<String, ByteArray> = emptyMap(), val mainClass: String? = null)

    fun compile(files: List<KtFile>): Compiled {
        val generationState = generationStateFor(files)
        KotlinCodegenFacade.compileCorrectFiles(generationState) { error, _ -> error.printStackTrace() }
        return Compiled(
                files = generationState.factory.asList().map { it.relativePath to it.asByteArray() }.toMap(),
                mainClass = mainClassFrom(generationState.bindingContext, files)
        )
    }

    private fun generationStateFor(files: List<KtFile>): GenerationState {
        val analysis = environment.analysisOf(files)
        return GenerationState.Builder(
            files.first().project,
            ClassBuilderFactories.BINARIES,
            analysis.analysisResult.moduleDescriptor,
            analysis.analysisResult.bindingContext,
            files,
            environment.kotlinEnvironment.configuration
        ).build()
    }

    private fun mainClassFrom(bindingContext: BindingContext, files: List<KtFile>): String? {
        val mainFunctionDetector = MainFunctionDetector(bindingContext, LanguageVersionSettingsImpl.DEFAULT)
        return files.find { mainFunctionDetector.hasMain(it.declarations) }?.let {
            PackagePartClassUtils.getPackagePartFqName(it.packageFqName, it.name).asString()
        }
    }
}