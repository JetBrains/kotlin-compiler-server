package com.compiler.server.compiler.components

import com.compiler.server.compiler.model.Analysis
import com.compiler.server.compiler.model.ErrorDescriptor
import com.compiler.server.compiler.model.Severity
import com.compiler.server.compiler.model.TextInterval
import com.intellij.openapi.Disposable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import java.io.File
import java.util.UUID
import kotlin.Comparator

class KotlinEnvironment(val classpath: List<File>, val coreEnvironment: KotlinCoreEnvironment) {

    fun errorsFrom(files: List<KtFile>): Map<String, List<ErrorDescriptor>> {
        return errorsFrom(analysisOf(files).analysisResult.bindingContext.diagnostics.all(), files.map { it.name to anylizeErrorsFrom(it) }.toMap())
    }

    fun analysisOf(files: List<KtFile>): Analysis = CliBindingTrace().let { trace ->
        val project = files.first().project
        val componentProvider = TopDownAnalyzerFacadeForJVM.createContainer(
                project,
                files,
                trace,
                coreEnvironment.configuration,
                { globalSearchScope -> coreEnvironment.createPackagePartProvider(globalSearchScope) },
                { storageManager, ktFiles -> FileBasedDeclarationProviderFactory(storageManager, ktFiles) },
                TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, files)
        )
        componentProvider.getService(LazyTopDownAnalyzer::class.java)
                .analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files, DataFlowInfo.EMPTY)
        val moduleDescriptor = componentProvider.getService(ModuleDescriptor::class.java)
        AnalysisHandlerExtension.getInstances(project)
                .find { it.analysisCompleted(project, moduleDescriptor, trace, files) != null }
        Analysis(
                componentProvider = componentProvider,
                analysisResult = AnalysisResult.success(trace.bindingContext, moduleDescriptor)
        )
    }

    private fun errorsFrom(
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
                            it.textRange.startOffset,
                            it.textRange.endOffset,
                            file.viewProvider.document!!
                    ), it.errorDescription, Severity.ERROR, "red_wavy_line"
            )
        }
    }

    private fun errorsFrom(diagnostics: Collection<Diagnostic>) = diagnostics.mapNotNull { diagnostic ->
        diagnostic.psiFile.virtualFile?.let {
            val render = DefaultErrorMessages.render(diagnostic)
            if (!render.contains("This cast can never succeed")) {
                if (diagnostic.severity != org.jetbrains.kotlin.diagnostics.Severity.INFO) {
                    val textRanges = diagnostic.textRanges.iterator()
                    if (textRanges.hasNext()) {
                        var className = diagnostic.severity.name
                        if (!(diagnostic.factory === Errors.UNRESOLVED_REFERENCE) && diagnostic.severity == org.jetbrains.kotlin.diagnostics.Severity.ERROR) {
                            className = "red_wavy_line"
                        }
                        val firstRange = textRanges.next()
                        val interval = TextInterval.from(firstRange.startOffset, firstRange.endOffset, diagnostic.psiFile.viewProvider.document!!)
                        diagnostic.psiFile.name to ErrorDescriptor(interval, render, Severity.from(diagnostic.severity), className)
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


    companion object {
        /**
         * This list allows to configure behavior of webdemo compiler. Its effect is equivalent
         * to passing this list of string to CLI compiler.
         *
         * See [org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments] and
         * [org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments] for list of possible flags
         */
        private val additionalCompilerArguments: List<String> = listOf(
                "-Xuse-experimental=kotlin.Experimental",
                "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes",
                "-Xuse-experimental=kotlin.contracts.ExperimentalContracts",
                "-Xuse-experimental=kotlin.experimental.ExperimentalTypeInference",
                "-XXLanguage:+InlineClasses"
        )

        fun with(classpath: List<File>): KotlinEnvironment {
            val arguments = K2JVMCompilerArguments()
            parseCommandLineArguments(additionalCompilerArguments, arguments)
            return KotlinEnvironment(classpath, KotlinCoreEnvironment.createForTests(
                    parentDisposable = Disposable {},
                    extensionConfigs = EnvironmentConfigFiles.JVM_CONFIG_FILES,
                    initialConfiguration = CompilerConfiguration().apply {
                        addJvmClasspathRoots(classpath.filter { it.exists() && it.isFile && it.extension == "jar" })
                        put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
                        put(JVMConfigurationKeys.ADD_BUILT_INS_FROM_COMPILER_TO_DEPENDENCIES, true)
                        put(CommonConfigurationKeys.MODULE_NAME, UUID.randomUUID().toString())
                        with(K2JVMCompilerArguments()) {
                            put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, noParamAssertions)
                            put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, noCallAssertions)
                        }
                        languageVersionSettings = arguments.configureLanguageVersionSettings(this[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]!!)
                    }
            ))
        }
    }
}
