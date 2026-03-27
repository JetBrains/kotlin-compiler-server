package com.compiler.server.compiler_arguments

import com.compiler.server.configuration.CompilerArgumentsConfiguration
import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames.jsArguments
import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames.jvmCompilerArguments
import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames.wasmArguments
import kotlin.test.Test
import kotlin.test.assertTrue

private const val COMPILER_ARGUMENTS_JSON = "kotlin-compiler-arguments.json"

class CompilerArgumentsConfigurationTest {

    private val compilerArgumentsConfiguration: CompilerArgumentsConfiguration = CompilerArgumentsConfiguration()

    @Test
    fun validateKotlinCompilerArgumentsSchema() {

        val kotlinTargetCompilerArgumentsByName = compilerArgumentsConfiguration.kotlinTargetCompilerArgumentsByName()

        assertTrue(
            kotlinTargetCompilerArgumentsByName.contains(jsArguments),
            "There are no JS arguments in $COMPILER_ARGUMENTS_JSON"
        )
        assertTrue(
            kotlinTargetCompilerArgumentsByName[jsArguments]!!.isNotEmpty(),
            "JS arguments are empty in $COMPILER_ARGUMENTS_JSON"
        )

        assertTrue(
            kotlinTargetCompilerArgumentsByName.contains(wasmArguments),
            "There are no WASM arguments in $COMPILER_ARGUMENTS_JSON"
        )
        assertTrue(
            kotlinTargetCompilerArgumentsByName[wasmArguments]!!.isNotEmpty(),
            "WASM arguments are empty in $COMPILER_ARGUMENTS_JSON"
        )

        assertTrue(
            kotlinTargetCompilerArgumentsByName.contains(jvmCompilerArguments),
            "There are no JVM arguments in $COMPILER_ARGUMENTS_JSON"
        )
        assertTrue(
            kotlinTargetCompilerArgumentsByName[jvmCompilerArguments]!!.isNotEmpty(),
            "JVM arguments are empty in $COMPILER_ARGUMENTS_JSON"
        )
    }
}