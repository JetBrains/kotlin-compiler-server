package com.compiler.server.compiler.components

import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.KotlinResolutionFacade
import com.compiler.server.model.Analysis
import com.compiler.server.model.Completion
import com.compiler.server.model.ImportInfo
import com.fasterxml.jackson.module.kotlin.isKotlinClass
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
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
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.jar.JarFile
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.kotlinFunction

@Component
class SuggestionProvider(
  private val errorAnalyzer: ErrorAnalyzer,
  private val kotlinEnvironment: KotlinEnvironment
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
  private val MODULE_INFO_NAME = "module-info"
  private val EXECUTORS_JAR_NAME = "executors.jar"
  private val ALL_INDEXES = getAllVariants()

  private data class DescriptorInfo(
    val isTipsManagerCompletion: Boolean,
    val descriptors: List<DeclarationDescriptor>
  )

  fun complete(
    file: KotlinFile,
    line: Int,
    character: Int,
    isJs: Boolean,
    coreEnvironment: KotlinCoreEnvironment
  ) = with(file.insert("IntellijIdeaRulezzz ", line, character)) {
    elementAt(line, character)?.let { element ->
      val descriptorInfo = descriptorsFrom(this, element, isJs, coreEnvironment)
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

  fun completeWithImport(
    file: KotlinFile,
    line: Int,
    character: Int,
    isJs: Boolean,
    coreEnvironment: KotlinCoreEnvironment
  ) = with(file.insert("IntellijIdeaRulezzz ", line, character)) {
    elementAt(line, character)?.let { element ->
      val descriptorInfo = descriptorsFrom(this, element, isJs, coreEnvironment)
      val prefix = (if (descriptorInfo.isTipsManagerCompletion) element.text else element.parent.text)
        .substringBefore("IntellijIdeaRulezzz").let { if (it.endsWith(".")) "" else it }
      getClassByPrefix(prefix)
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
    } else null
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


  private fun Analysis.referenceVariantsFrom(
    element: PsiElement,
    coreEnvironment: KotlinCoreEnvironment
  ): List<DeclarationDescriptor>? {
    val elementKt = element as? KtElement ?: return emptyList()
    val bindingContext = analysisResult.bindingContext
    val resolutionFacade = KotlinResolutionFacade(
      project = coreEnvironment.project,
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

  private fun descriptorsFrom(
    file: KotlinFile,
    element: PsiElement,
    isJs: Boolean,
    coreEnvironment: KotlinCoreEnvironment
  ): DescriptorInfo {
    val files = listOf(file.kotlinFile)
    val analysis = if (isJs.not())
      errorAnalyzer.analysisOf(files, coreEnvironment)
    else
      errorAnalyzer.analyzeFileForJs(files, coreEnvironment)
    return with(analysis) {
      (referenceVariantsFrom(element, coreEnvironment)
        ?: referenceVariantsFrom(element.parent, coreEnvironment))?.let { descriptors ->
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

  private fun allClassesFromJavaClass(clazz: Class<*>): HashSet<ImportInfo> {
    val allClasses = hashSetOf<ImportInfo>()
    clazz.classes.filter {
      Modifier.isPublic(it.modifiers)
    }.map {
      val canonicalName = it.canonicalName
      val simpleName = it.simpleName
      val importInfo = ImportInfo(canonicalName, simpleName)
      allClasses.add(importInfo)
    }
    return allClasses
  }

  private fun allClassesFromKotlinClass(clazz: Class<*>): HashSet<ImportInfo> {
    val allClasses = hashSetOf<ImportInfo>()
    val kotlinClass = clazz.kotlin
    try {
      kotlinClass.nestedClasses.filter {
        it.visibility == KVisibility.PUBLIC
      }.map {
        val canonicalName = it.qualifiedName ?: ""
        val simpleName = it.simpleName ?: ""
        val importInfo = ImportInfo(canonicalName, simpleName)
        allClasses.add(importInfo)
      }
      if (kotlinClass.visibility == KVisibility.PUBLIC) {
        val canonicalName = kotlinClass.qualifiedName ?: ""
        val simpleName = kotlinClass.simpleName ?: ""
        val importInfo = ImportInfo(canonicalName, simpleName)
        allClasses.add(importInfo)
      }
    } catch (exception: UnsupportedOperationException) {
      // throw kotlinClass.nestedClasses
      return allClassesFromJavaClass(clazz)
    } catch (error: AssertionError) {
      /* In Tests:
      1) kotlinx.coroutines.flow.internal.ChannelFlowKt$withContextUndispatched$$inlined
        $suspendCoroutineUninterceptedOrReturn$lambda$1
      2) kotlin.coroutines.experimental.intrinsics.IntrinsicsKt__IntrinsicsJvmKt
        $createCoroutineUnchecked$$inlined$buildContinuationByInvokeCall$IntrinsicsKt__IntrinsicsJvmKt$1
      3) kotlin.coroutines.experimental.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnchecked
        $$inlined$buildContinuationByInvokeCall$IntrinsicsKt__IntrinsicsJvmKt$2
      */
      return allClassesFromJavaClass(clazz)
    } catch (error: IncompatibleClassChangeError) {
      /* In Application:
      1) kotlinx.coroutines.CoroutineDispatcher$Key
      2) kotlinx.coroutines.channels.ConflatedChannel$Companion
      3) kotlinx.coroutines.ExecutorCoroutineDispatcher$Key
      */
      return allClassesFromJavaClass(clazz)
    } catch (error: InternalError) {
      /* In Application:
      1-53) kotlinx.coroutines.flow.FlowKt__*
      54-59) kotlinx.coroutines.channels.*
       */
      return allClassesFromJavaClass(clazz)
    } catch (exception: NoSuchElementException) {
      /* In Application:
      1) kotlinx.coroutines.flow.internal.ChannelFlowKt$withContextUndispatched$$inlined
        $suspendCoroutineUninterceptedOrReturn$lambda$1
      2) kotlin.coroutines.experimental.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnchecked
        $$inlined$buildContinuationByInvokeCall$IntrinsicsKt__IntrinsicsJvmKt$1
      3) kotlin.coroutines.experimental.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnchecked
        $$inlined$buildContinuationByInvokeCall$IntrinsicsKt__IntrinsicsJvmKt$2
      */
      return allClassesFromJavaClass(clazz)
    }
    return allClasses
  }

  private fun getVariantsForZip(file: File): List<ImportInfo> {
    val jarFile = JarFile(file)
    val allSuggests = hashSetOf<ImportInfo>()
    jarFile.entries().toList().map { entry ->
      if (!entry.isDirectory
        && entry.name.endsWith(".class")
      ) {
        val name = entry.name.removeSuffix(".class")
        val fullName = name.replace("/", ".")
        if (fullName != MODULE_INFO_NAME) {
          try {
            val clazz = ClassLoader.getSystemClassLoader().loadClass(fullName)
            if (clazz.isKotlinClass()) {
              allSuggests.addAll(allClassesFromKotlinClass(clazz))
            } else {
              allSuggests.addAll(allClassesFromJavaClass(clazz))
            }
            allSuggests.addAll(allFunctionsFromClass(clazz))
          } catch (error: VerifyError) {
          /* In Application:
            1) kotlinx.coroutines.EventLoopImplBase$DelayedTaskQueue
            2) kotlinx.coroutines.flow.internal.FlowProduceCoroutine
            */
          } catch (error: NoClassDefFoundError) {
            /* In Application:
            1) kotlinx.coroutines.flow.FlowKt__MergeKt$flatMapLatest$1
            2) kotlinx.coroutines.flow.FlowKt__MergeKt$mapLatest$1
            3) kotlinx.coroutines.flow.FlowKt__MergeKt$flattenConcat$$inlined$unsafeFlow$1
            4) kotlinx.coroutines.flow.FlowKt__MergeKt$flatMapConcat$$inlined$map$1
            5) kotlinx.coroutines.flow.FlowKt__MergeKt$flatMapMerge$$inlined$map$1
             */
          }
        }
      }
    }
    return allSuggests.toList()
  }

  private fun allFunctionsFromClass(clazz: Class<*>): HashSet<ImportInfo> {
    val allClasses = hashSetOf<ImportInfo>()
    clazz.declaredMethods.map { method ->
      val importInfo = importInfoFromFunction(method, clazz)
      if (importInfo != null) allClasses.add(importInfo)
    }
    return allClasses
  }

  private fun importInfoFromFunction(method: Method, clazz: Class<*>): ImportInfo? {
    var kotlinFunction : KFunction<*>? = null
    try {
      kotlinFunction = method.kotlinFunction
    } catch (exception: NoSuchElementException) {
    } catch (exception: UnsupportedOperationException) {
    } catch (error: KotlinReflectionInternalError) {
    } catch (error: AssertionError) {
    } catch (error: InternalError) {
    } catch (error: IncompatibleClassChangeError) {
    }
    return if (kotlinFunction != null
      && kotlinFunction.visibility == KVisibility.PUBLIC) {
      importInfoByMethodAndParent(
        kotlinFunction.name,
        kotlinFunction.parameters.map { it.type }.joinToString(),
        clazz)
    } else importInfoFromJavaMethod(method, clazz)
  }

  private fun importInfoFromJavaMethod(method: Method, clazz: Class<*>): ImportInfo? =
    if (Modifier.isPublic(method.modifiers) &&
      Modifier.isStatic(method.modifiers))
      importInfoByMethodAndParent(method.name, method.parameters.map { it.type.name }.joinToString(), clazz)
    else null

  private fun importInfoByMethodAndParent(methodName: String, parametersString: String, parent: Class<*>): ImportInfo {
    val shortName = methodName.split("$").first()
    val className = "$shortName($parametersString)"
    val fullName = "${parent.`package`.name}.$shortName"
    return ImportInfo(fullName, className)
  }

  private fun getAllVariants(): List<ImportInfo> {
    val jarFiles = kotlinEnvironment.classpath.filter { jarFile ->
      jarFile.name.split("/").last() != EXECUTORS_JAR_NAME
    }

    val allVariants = mutableListOf<ImportInfo>()
    jarFiles.map { file ->
      val variants = getVariantsForZip(file)
      allVariants.addAll(variants)
    }
    return allVariants
  }

  fun getClassesByName(name: String): List<ImportInfo> {
    return getAllVariants().filter { variant ->
      variant.className == name
    }
  }

  fun getClassByPrefix(prefix: String): List<ImportInfo> {
    return ALL_INDEXES.filter { variant ->
      variant.className.startsWith(prefix)
    }.sortedBy { it.className.length }
  }
}