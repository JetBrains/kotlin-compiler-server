package indexation

import model.ImportInfo
import model.Icon
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.asFlexibleType
import org.jetbrains.kotlin.types.isFlexible

internal val renderer = IdeDescriptorRenderers.SOURCE_CODE.withOptions {
  classifierNamePolicy = ClassifierNamePolicy.SHORT
  typeNormalizer = IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
  parameterNameRenderingPolicy = ParameterNameRenderingPolicy.ALL
  typeNormalizer = {
    if (it.isFlexible()) it.asFlexibleType().upperBound
    else it
  }
}

internal fun DeclarationDescriptor.toImportInfo(): ImportInfo? {
  val importName = importableFqName?.asString() ?: return null
  if (name.asString() == "Companion") return null
  return when (this) {
    is FunctionDescriptor -> {
      if (visibility.isPublicAPI) {
      val returnTypeVal = if (returnType != null)  renderer.renderType(returnType!!)
          else {extensionReceiverParameter?.let { param ->
            " for ${renderer.renderType(param.type)} in ${DescriptorUtils.getFqName(containingDeclaration)}"
          } ?: "" }
      ImportInfo(
        importName = importName,
        shortName = name.asString(),
        fullName = name.asString() + renderer.renderFunctionParameters(this),
        returnType = returnTypeVal,
        icon = Icon.METHOD
      ) } else null
    }

    is ClassDescriptor -> {
      if (visibility.isPublicAPI) {
        ImportInfo(
          importName = importName,
          shortName = name.asString(),
          fullName = name.asString(),
          returnType = name.asString(),
          icon = Icon.CLASS
        )
      } else null
    }

    is PropertyDescriptor -> {
      ImportInfo(
        importName = importName,
        shortName = name.asString(),
        fullName = name.asString(),
        returnType = name.asString(),
        icon = Icon.PROPERTY
      )
    }

    else -> null
  }
}

internal fun DeclarationDescriptor.getInnerClassesAndAllStaticFunctions(): List<DeclarationDescriptor>? {
  return if (this !is ClassDescriptor || !visibility.isPublicAPI)
    null
  else (unsubstitutedInnerClassesScope.getContributedDescriptors(DescriptorKindFilter.ALL, MemberScope.ALL_NAME_FILTER) +
    staticScope.getContributedDescriptors(DescriptorKindFilter.ALL, MemberScope.ALL_NAME_FILTER)).distinct()
}

internal fun ModuleDescriptor.allImportsInfo(): List<ImportInfo> {
  val packages = allPackages()
  return packages.flatMap { fqName ->
    val packageViewDescriptor = getPackage(fqName)
    val descriptors = packageViewDescriptor.memberScope
      .getContributedDescriptors(DescriptorKindFilter.ALL, MemberScope.ALL_NAME_FILTER)
    val allDescriptors = descriptors + descriptors.mapNotNull { it.getInnerClassesAndAllStaticFunctions() }.flatten()
    allDescriptors.mapNotNull { it.toImportInfo() }
  }
}

internal fun ModuleDescriptor.allPackages(): Collection<FqName> {
  val result = mutableListOf<FqName>()
  fun impl(pkg: FqName) {
    result += pkg

    getSubPackagesOf(pkg) { true }.forEach { impl(it) }
  }
  impl(FqName.ROOT)
  return result
}
