package com.compiler.server.validation

import com.compiler.server.model.BooleanExtendedCompilerArgumentValue
import com.compiler.server.model.ExtendedCompilerArgument
import com.compiler.server.model.ExtendedCompilerArgumentValue
import com.compiler.server.model.ListExtendedCompilerArgumentValue
import com.compiler.server.model.StringExtendedCompilerArgumentValue
import jakarta.validation.ConstraintValidatorContext

/**
 * @param knownCompilerArguments The set of predefined compiler arguments that are
 * recognized and supported.
 */
abstract class AbstractCompilerArgumentsValidator(private val knownCompilerArguments: Set<ExtendedCompilerArgument>) {
    private val STRING_ARGUMENT_REGEX = Regex("^[}{A-Za-z0-9+-.,:=]+\$")

    fun validateCompilerArguments(
        compilerArguments: Map<String, Any>,
    ): Boolean {
        if (compilerArguments.isEmpty()) return true

        if (isCompilerArgumentsKeysValid(compilerArguments.keys).not()) {
            return false
        }
        if (isCompilerArgumentsValuesValid(compilerArguments).not()) {
            return false
        }
        return true
    }

    private fun isCompilerArgumentsValuesValid(compilerArguments: Map<String, Any>): Boolean {
        for ((argumentName, argumentValue) in compilerArguments) {
            if (isCompilerArgumentValueValid(argumentName, argumentValue).not()) {
                return false
            }
        }
        return true
    }

    private fun isCompilerArgumentValueValid(argumentName: String, argumentValue: Any): Boolean {
        val expectedArgumentType = knownCompilerArguments
            .find { it.name == argumentName }
            ?.type ?: throw IllegalArgumentException("Unknown compiler argument: $argumentName")

        return when (argumentValue) {
            is Boolean -> expectedArgumentType is BooleanExtendedCompilerArgumentValue

            is List<*> -> expectedArgumentType is ListExtendedCompilerArgumentValue &&
                    checkListJvmCompilerArgument(argumentName, argumentValue)

            is String -> expectedArgumentType is StringExtendedCompilerArgumentValue &&
                    checkStringJvmCompilerArgument(argumentName, argumentValue)

            else -> false // unsupported type
        }
    }

    private fun isCompilerArgumentsKeysValid(
        keys: Set<String>,
    ): Boolean {
        for (argumentKey in keys) {
            val knownCompilerArgument = knownCompilerArguments.find { it.name == argumentKey }
            if (knownCompilerArgument == null) {
                return false
            }
            if (knownCompilerArgument.disabled) {
                return false
            }
        }
        return true
    }

    fun checkListJvmCompilerArgument(argumentName: String, argumentValues: List<*>): Boolean {
        argumentValues.forEach { argumentValue ->
            if (argumentValue !is String) return false
            if (checkStringJvmCompilerArgument(argumentName, argumentValue).not()) return false
        }
        return true
    }

    fun checkStringJvmCompilerArgument(argumentName: String, argumentValue: String): Boolean {
        if (!STRING_ARGUMENT_REGEX.matches(argumentValue)) return false
        if (argumentValue.any { it.isWhitespace() }) return false
        if (argumentName != "XXLanguage" && argumentValue.startsWith("-")) return false
        return true
    }
}