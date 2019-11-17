package com.compiler.server.compiler.components

import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.KotlinResolutionFacade
import com.compiler.server.model.Analysis
import com.compiler.server.model.Completion
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.asFlexibleType
import org.jetbrains.kotlin.types.isFlexible
import org.springframework.stereotype.Component


@Component
class CompletionProvider(
  private val kotlinEnvironment: KotlinEnvironment,
  private val errorAnalyzer: ErrorAnalyzer
) {

  private val excludedFromCompletion: List<String> = listOf(
    "kotlin.jvm.internal",
    "kotlin.coroutines.experimental.intrinsics",
    "kotlin.coroutines.intrinsics",
    "kotlin.coroutines.experimental.jvm.internal",
    "kotlin.coroutines.jvm.internal",
    "kotlin.reflect.jvm.internal"
  )
  private val NUMBER_OF_CHAR_IN_TAIL = 60
  private val NUMBER_OF_CHAR_IN_COMPLETION_NAME = 40
  private val NAME_FILTER = { name: Name -> !name.isSpecial }

  private data class DescriptorInfo(
    val isTipsManagerCompletion: Boolean,
    val descriptors: List<DeclarationDescriptor>
  )

  fun complete(
    file: KotlinFile,
    line: Int,
    character: Int,
    isJs: Boolean
  ) = with(file.insert("IntellijIdeaRulezzz ", line, character)) {
    elementAt(line, character)?.let { element ->
      val descriptorInfo = descriptorsFrom(this, element, isJs)
      val prefix = (if (descriptorInfo.isTipsManagerCompletion) element.text else element.parent.text)
        .substringBefore("IntellijIdeaRulezzz").let { if (it.endsWith(".")) "" else it }
      descriptorInfo.descriptors.toMutableList().apply {
        sortWith(Comparator { a, b ->
          val (a1, a2) = a.presentableName()
          val (b1, b2) = b.presentableName()
          ("$a1$a2").compareTo("$b1$b2", true)
        })
      }.mapNotNull { descriptor -> completionVariantFor(prefix, descriptor, element) } + keywordsCompletionVariants(KtTokens.KEYWORDS,
                                                                                                                    prefix) + keywordsCompletionVariants(
        KtTokens.SOFT_KEYWORDS, prefix)
    } ?: emptyList()
  }

  private fun DeclarationDescriptor.presentableName(isCallableReferenceCompletion: Boolean = false): Pair<String, String> {
    var presentableText = if (this is ConstructorDescriptor)
      this.constructedClass.name.asString()
    else
      this.name.asString()
    return when (this) {
      is FunctionDescriptor -> {
        if (!isCallableReferenceCompletion)
          presentableText += renderer.renderFunctionParameters(this)
        presentableText to when {
          returnType != null -> renderer.renderType(returnType!!)
          else -> (extensionReceiverParameter?.let { param ->
            " for ${renderer.renderType(param.type)} in ${DescriptorUtils.getFqName(containingDeclaration)}"
          } ?: "")
        }
      }
      else -> presentableText to when (this) {
        is VariableDescriptor -> renderer.renderType(type)
        is ClassDescriptor -> " (${DescriptorUtils.getFqName(containingDeclaration)})"
        else -> renderer.render(this)
      }
    }
  }


  private fun completionVariantFor(
    prefix: String,
    descriptor: DeclarationDescriptor,
    element: PsiElement
  ): Completion? {
    val isCallableReference = (element as? KtElement)?.isCallableReference() ?: false
    val (name, _) = descriptor.presentableName(isCallableReference)
    val fullName: String = formatName(name, NUMBER_OF_CHAR_IN_COMPLETION_NAME)
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
      Completion(
        text = completionText,
        displayText = fullName,
        tail = formatName(fullName, NUMBER_OF_CHAR_IN_TAIL),
        icon = iconFrom(descriptor)
      )
    }
    else null
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


  private fun Analysis.referenceVariantsFrom(element: PsiElement): List<DeclarationDescriptor>? {
    val elementKt = element as? KtElement ?: return emptyList()
    val bindingContext = analysisResult.bindingContext
    val resolutionFacade = KotlinResolutionFacade(
      project = kotlinEnvironment.coreEnvironment.project,
      componentProvider = componentProvider,
      moduleDescriptor = analysisResult.moduleDescriptor
    )
    val inDescriptor: DeclarationDescriptor = elementKt.getResolutionScope(bindingContext, resolutionFacade).ownerDescriptor
    return when (element) {
      is KtSimpleNameExpression -> ReferenceVariantsHelper(
        bindingContext = analysisResult.bindingContext,
        resolutionFacade = resolutionFacade,
        moduleDescriptor = analysisResult.moduleDescriptor,
        visibilityFilter = VisibilityFilter(inDescriptor, bindingContext, element, resolutionFacade)
      ).getReferenceVariants(
        expression = element,
        kindFilter = DescriptorKindFilter.ALL,
        nameFilter = NAME_FILTER,
        filterOutJavaGettersAndSetters = true,
        filterOutShadowed = true,
        excludeNonInitializedVariable = true,
        useReceiverType = null).toList()
      else -> null
    }
  }

  private fun descriptorsFrom(file: KotlinFile, element: PsiElement, isJs: Boolean): DescriptorInfo {
    val files = listOf(file.kotlinFile)
    val analysis = if (isJs.not()) errorAnalyzer.analysisOf(files) else errorAnalyzer.analyzeFileForJs(files)
    return with(analysis) {
      (referenceVariantsFrom(element) ?: referenceVariantsFrom(element.parent))?.let { descriptors ->
        DescriptorInfo(true, descriptors)
      } ?: element.parent.let { parent ->
        DescriptorInfo(
          isTipsManagerCompletion = false,
          descriptors = when (parent) {
            is KtQualifiedExpression -> {
              analysisResult.bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, parent.receiverExpression)
                ?.type?.let { expressionType ->
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
  }


  private fun formatName(
    builder: String,
    symbols: Int
  ) = if (builder.length > symbols) builder.substring(0, symbols) + "..." else builder

  private fun keywordsCompletionVariants(keywords: TokenSet, prefix: String) = keywords.types.mapNotNull {
    if (it is KtKeywordToken && it.value.startsWith(prefix)) Completion(it.value, it.value, "", "") else null
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

  private fun KtElement.isCallableReference() =
    parent is KtCallableReferenceExpression && this == (parent as KtCallableReferenceExpression).callableReference

  // This code is a fragment of org.jetbrains.kotlin.idea.completion.CompletionSession from Kotlin IDE Plugin
  // with a few simplifications which were possible because webdemo has very restricted environment (and well,
  // because requirements on compeltion' quality in web-demo are lower)
  private inner class VisibilityFilter(
    private val inDescriptor: DeclarationDescriptor,
    private val bindingContext: BindingContext,
    private val element: KtElement,
    private val resolutionFacade: KotlinResolutionFacade
  ) : (DeclarationDescriptor) -> Boolean {
    override fun invoke(descriptor: DeclarationDescriptor): Boolean {
      if (descriptor is TypeParameterDescriptor && !isTypeParameterVisible(descriptor)) return false

      if (descriptor is DeclarationDescriptorWithVisibility) {
        return descriptor.isVisible(element, null, bindingContext, resolutionFacade)
      }

      if (descriptor.isInternalImplementationDetail()) return false

      return true
    }

    private fun isTypeParameterVisible(typeParameter: TypeParameterDescriptor): Boolean {
      val owner = typeParameter.containingDeclaration
      var parent: DeclarationDescriptor? = inDescriptor
      while (parent != null) {
        if (parent == owner) return true
        if (parent is ClassDescriptor && !parent.isInner) return false
        parent = parent.containingDeclaration
      }
      return true
    }

    private fun DeclarationDescriptor.isInternalImplementationDetail(): Boolean =
      importableFqName?.asString() in excludedFromCompletion
  }
}