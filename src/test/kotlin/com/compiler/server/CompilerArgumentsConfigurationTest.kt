package com.compiler.server

import com.compiler.server.configuration.CompilerArgumentsConfiguration
import com.compiler.server.utils.COMMON_ARGUMENTS_NAME
import com.compiler.server.utils.COMMON_KLIB_BASED_ARGUMENTS_NAME
import com.compiler.server.utils.COMPILER_ARGUMENTS_JSON
import com.compiler.server.utils.JVM_ARGUMENTS_NAME
import com.compiler.server.utils.METADATA_ARGUMENTS_NAME
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompilerArgumentsConfigurationTest {

    private val compilerArgumentsConfiguration: CompilerArgumentsConfiguration = CompilerArgumentsConfiguration()


    @Test
    fun validateKotlinCompilerArgumentsJsonFile() {
        val kotlinCompilerArguments = compilerArgumentsConfiguration.kotlinCompilerArguments()

        assertEquals(
            2, kotlinCompilerArguments.schemaVersion,
            "Unsupported schema version of $COMPILER_ARGUMENTS_JSON"
        )

        assertEquals("commonToolArguments", kotlinCompilerArguments.topLevel.name)

        assertEquals(
            1,
            kotlinCompilerArguments.topLevel.nestedLevels.size,
            "Unexpected top-level's nested levels count"
        )

        val argumentsFirstLayer = kotlinCompilerArguments
            .topLevel
            .nestedLevels
            .first()
        assertEquals(COMMON_ARGUMENTS_NAME, argumentsFirstLayer.name)

        assertEquals(3, argumentsFirstLayer.nestedLevels.size)

        val actualSecondLevelArgumentNames = argumentsFirstLayer.nestedLevels.map { it.name }
        val expectedSecondLevelArgumentNames =
            listOf(JVM_ARGUMENTS_NAME, COMMON_KLIB_BASED_ARGUMENTS_NAME, METADATA_ARGUMENTS_NAME)
        assertTrue(
            actualSecondLevelArgumentNames.containsAll(expectedSecondLevelArgumentNames),
            "Unexpected second-level's arguments names. Expected: $expectedSecondLevelArgumentNames, got: $actualSecondLevelArgumentNames"
        )
    }
}