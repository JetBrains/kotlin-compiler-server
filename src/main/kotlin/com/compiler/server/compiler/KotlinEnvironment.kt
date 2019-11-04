package com.compiler.server.compiler

import com.compiler.server.compiler.model.*
import com.intellij.openapi.Disposable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.asFlexibleType
import org.jetbrains.kotlin.types.isFlexible
import java.io.File
import java.util.UUID
import kotlin.Comparator

class KotlinEnvironment(val classpath: List<File>, val kotlinEnvironment: KotlinCoreEnvironment) {

    private data class DescriptorInfo(
            val isTipsManagerCompletion: Boolean,
            val descriptors: List<DeclarationDescriptor>
    )

    fun complete(
            file: KotlinFile,
            line: Int,
            character: Int
    ) = with(file.insert("IntellijIdeaRulezzz ", line, character)) {
        elementAt(line, character)?.let { element ->
            val descriptorInfo = descriptorsFrom(this, element)
            val prefix = (if (descriptorInfo.isTipsManagerCompletion) element.text else element.parent.text)
                    .substringBefore("IntellijIdeaRulezzz").let { if (it.endsWith(".")) "" else it }
            descriptorInfo.descriptors.toMutableList().apply {
                sortWith(Comparator { a, b ->
                    val (a1, a2) = a.presentableName()
                    val (b1, b2) = b.presentableName()
                    ("$a1$a2").compareTo("$b1$b2", true)
                })
            }.mapNotNull { descriptor -> completionVariantFor(prefix, descriptor) } + keywordsCompletionVariants(KtTokens.KEYWORDS, prefix) + keywordsCompletionVariants(KtTokens.SOFT_KEYWORDS, prefix)
        } ?: emptyList()
    }

    fun errorsFrom(files: List<KtFile>): Map<String, List<ErrorDescriptor>> {
        return errorsFrom(analysisOf(files).analysisResult.bindingContext.diagnostics.all(), files.map { it.name to anylizeErrorsFrom(it) }.toMap())
    }

    private fun completionVariantFor(prefix: String, descriptor: DeclarationDescriptor): Completion? {
        val (name, tail) = descriptor.presentableName()
        val fullName: String = formatName(name, 40)
        var completionText = fullName
        var position = completionText.indexOf('(')
        if (position != -1) {
            if (completionText[position - 1] == ' ') position -= 2
            if (completionText[position + 1] == ')') position++
            completionText = completionText.substring(0, position + 1)
        }
        position = completionText.indexOf(":")
        if (position != -1) completionText = completionText.substring(0, position - 1)
        return if (prefix.isEmpty() || fullName.startsWith(prefix)) {
            Completion(completionText, fullName, tail, iconFrom(descriptor))
        } else null
    }

    private fun DeclarationDescriptor.presentableName() = when (this) {
        is FunctionDescriptor -> name.asString() + renderer.renderFunctionParameters(this) to when {
            returnType != null -> renderer.renderType(returnType!!)
            else -> (extensionReceiverParameter?.let { param ->
                " for ${renderer.renderType(param.type)} in ${DescriptorUtils.getFqName(containingDeclaration)}"
            } ?: "")
        }
        else -> name.asString() to when (this) {
            is VariableDescriptor -> renderer.renderType(type)
            is ClassDescriptor -> " (${DescriptorUtils.getFqName(containingDeclaration)})"
            else -> renderer.render(this)
        }
    }

    private val renderer = IdeDescriptorRenderers.SOURCE_CODE.withOptions {
        classifierNamePolicy = ClassifierNamePolicy.SHORT
        typeNormalizer = IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
        parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
        typeNormalizer = {
            if (it.isFlexible()) it.asFlexibleType().upperBound
            else it
        }
    }

    private fun iconFrom(descriptor: DeclarationDescriptor) = when (descriptor) {
        is FunctionDescriptor -> "method"
        is PropertyDescriptor -> "property"
        is LocalVariableDescriptor -> "property"
        is ClassDescriptor -> "class"
        is PackageFragmentDescriptor -> "package"
        is PackageViewDescriptor -> "package"
        is ValueParameterDescriptor -> "genericValue"
        is TypeParameterDescriptorImpl -> "class"
        else -> ""
    }

    private fun formatName(
            builder: String,
            symbols: Int
    ) = if (builder.length > symbols) builder.substring(0, symbols) + "..." else builder

    private fun keywordsCompletionVariants(keywords: TokenSet, prefix: String) = keywords.types.mapNotNull {
        if (it is KtKeywordToken && it.value.startsWith(prefix)) Completion(it.value, it.value, "", "") else null
    }

    private fun Analysis.referenceVariantsFrom(element: PsiElement) = when (element) {
        is KtSimpleNameExpression -> ReferenceVariantsHelper(
                analysisResult.bindingContext,
                resolutionFacade = KotlinResolutionFacade(kotlinEnvironment.project, componentProvider),
                moduleDescriptor = analysisResult.moduleDescriptor,
                visibilityFilter = { true }
        ).getReferenceVariants(element, DescriptorKindFilter.ALL, { true }, true, true, true, null).toList()
        else -> null
    }

    private fun descriptorsFrom(file: KotlinFile, element: PsiElement): DescriptorInfo =
            with(analysisOf(listOf(file.kotlinFile))) {
                (referenceVariantsFrom(element) ?: referenceVariantsFrom(element.parent))?.let { descriptors ->
                    DescriptorInfo(true, descriptors)
                } ?: element.parent.let { parent ->
                    DescriptorInfo(
                            isTipsManagerCompletion = false,
                            descriptors = when (parent) {
                                is KtQualifiedExpression -> {
                                    analysisResult.bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, parent.receiverExpression)?.type?.let { expressionType ->
                                        analysisResult.bindingContext.get(BindingContext.LEXICAL_SCOPE, parent.receiverExpression)?.let {
                                            expressionType.memberScope.getContributedDescriptors(DescriptorKindFilter.ALL, MemberScope.ALL_NAME_FILTER)
                                        }
                                    }?.toList() ?: emptyList()
                                }
                                else -> analysisResult.bindingContext.get(BindingContext.LEXICAL_SCOPE, element as KtExpression)
                                        ?.getContributedDescriptors(DescriptorKindFilter.ALL, MemberScope.ALL_NAME_FILTER)
                                        ?.toList() ?: emptyList()
                            }
                    )
                }
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

    fun analysisOf(files: List<KtFile>): Analysis = CliBindingTrace().let { trace ->
        val project = files.first().project
        val componentProvider = TopDownAnalyzerFacadeForJVM.createContainer(
                kotlinEnvironment.project,
                files,
                trace,
                kotlinEnvironment.configuration,
                { globalSearchScope -> kotlinEnvironment.createPackagePartProvider(globalSearchScope) },
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

    companion object {
        fun with(classpath: List<File>) = KotlinEnvironment(classpath, KotlinCoreEnvironment.createForTests(
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
                }
        ))
    }
}
