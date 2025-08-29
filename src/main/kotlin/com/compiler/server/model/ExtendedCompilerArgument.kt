package com.compiler.server.model

/**
 * This is a service layer representation of kotlin compiler arguments.
 */
data class ExtendedCompilerArgument(
    val name: String,
    val shortName: String?,
    val description: String?,
    val type: ExtendedCompilerArgumentValue<*>,
    val disabled: Boolean,
    val predefinedValues: Any?,
    val supportedOnCurrentVersion: Boolean
)

/**
 * This sealed interface represents all possible types of compiler arguments.
 */
sealed interface ExtendedCompilerArgumentValue<T : Any> {
    val isNullable: Boolean
    val defaultValue: T?
}

data class BooleanExtendedCompilerArgumentValue(
    override val isNullable: Boolean,
    override val defaultValue: Boolean?
) : ExtendedCompilerArgumentValue<Boolean>

data class StringExtendedCompilerArgumentValue(
    override val isNullable: Boolean,
    override val defaultValue: String?
) : ExtendedCompilerArgumentValue<String>

data class ListExtendedCompilerArgumentValue(
    override val isNullable: Boolean,
    override val defaultValue: List<*>
) : ExtendedCompilerArgumentValue<List<*>>
