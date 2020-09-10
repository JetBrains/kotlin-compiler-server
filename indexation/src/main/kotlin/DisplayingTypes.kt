package indexation

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.FqName
import kotlin.reflect.KType

private val KOTLIN_TYPE_PREFIX_REGEX = Regex("(kotlin\\.)([A-Z])") // regex for prefix for simple kotlin type, like Double, Any...

private val primitiveToPrimitiveType = mapOf(
  "int" to PrimitiveType.INT,
  "long" to PrimitiveType.LONG,
  "double" to PrimitiveType.DOUBLE,
  "float" to PrimitiveType.FLOAT,
  "boolean" to PrimitiveType.BOOLEAN,
  "char" to PrimitiveType.CHAR,
  "byte" to PrimitiveType.BYTE,
  "short" to PrimitiveType.SHORT
)

internal fun javaTypeToKotlin(type: Class<*>): String {
  return when {
    type.isPrimitive -> primitiveToPrimitiveType[type.simpleName]?.typeName?.asString() ?: type.simpleName
    type.isArray -> {
      val componentType = type.componentType
      if (componentType.isPrimitive) {
        primitiveToPrimitiveType[componentType.simpleName]?.arrayTypeName?.asString() ?: type.simpleName
      } else {
        return "Array<${javaTypeToKotlin(componentType)}>"
      }
    }
    else -> JavaToKotlinClassMap.mapJavaToKotlin(FqName(type.canonicalName))?.shortClassName?.asString() ?: type.simpleName
  }
}

internal fun kotlinTypeToType(kotlinType: KType): String {
  var type = kotlinType.toString()
  var range: IntRange?
  do {
    range = KOTLIN_TYPE_PREFIX_REGEX.find(type)?.groups?.get(1)?.range
    type = if (range != null) type.removeRange(range) else type
  } while (range != null)
  return type
}
