package indexation

import common.model.ImportInfo
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.DescriptorUtils
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
        icon = "method"
      ) } else null
    }

    is ClassDescriptor -> {
      if (visibility.isPublicAPI) {
        ImportInfo(
          importName = importName,
          shortName = name.asString(),
          fullName = name.asString(),
          returnType = name.asString(),
          icon = "class"
        )
      } else null
    }
    else -> null
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