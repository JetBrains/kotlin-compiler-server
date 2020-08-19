package indexation

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.FqName
import kotlin.reflect.KType

private const val KOTLIN_TYPE_PREFIX = "(kotlin\\.)([A-Z])" // prefix for simple kotlin type, like Double, Any...

private val primitiveToPrimitiveType: Map<String, PrimitiveType> = mapOf(
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
  if (type.isPrimitive) {
    return primitiveToPrimitiveType[type.simpleName]?.typeName?.asString() ?: type.simpleName
  } else if (type.isArray) {
    val componentType = type.componentType
    return if (componentType.isPrimitive) {
      primitiveToPrimitiveType[componentType.simpleName]?.arrayTypeName?.asString() ?: type.simpleName
    } else {
      return "Array<${javaTypeToKotlin(componentType)}>"
    }
  }
  return JavaToKotlinClassMap.mapJavaToKotlin(FqName(type.canonicalName))?.shortClassName?.asString() ?: type.simpleName
}

internal fun kotlinTypeToType(kotlinType: KType): String {
  var type = kotlinType.toString()
  val regex = Regex(KOTLIN_TYPE_PREFIX)
  var range: IntRange?
  do {
    range = regex.find(type)?.groups?.get(1)?.range
    type = if (range != null) type.removeRange(range) else type
  } while (range != null)
  return type
}
