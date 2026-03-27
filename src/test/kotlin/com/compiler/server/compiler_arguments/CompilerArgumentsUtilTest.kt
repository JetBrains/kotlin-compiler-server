package com.compiler.server.compiler_arguments

import com.compiler.server.utils.CompilerArgumentsUtil
import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames.jsArguments
import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames.jvmCompilerArguments
import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames.wasmArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test
import kotlin.test.assertTrue

@SpringBootTest
class CompilerArgumentsUtilTest {
    @Autowired
    private lateinit var compilerArgumentsUtil: CompilerArgumentsUtil

    @Autowired
    private lateinit var kotlinTargetCompilerArgumentsByName: Map<String, Set<KotlinCompilerArgument>>

    @Test
    fun checkJvmPredefinedCompilerArgumentsExist() {
        val jvmArgumentNames = kotlinTargetCompilerArgumentsByName[jvmCompilerArguments]?.map { it.name } ?: emptyList()
        val predefinedJvmArgumentNames = compilerArgumentsUtil.PREDEFINED_JVM_ARGUMENTS.keys
        assertTrue(
            jvmArgumentNames.containsAll(predefinedJvmArgumentNames),
            "Some predefined arguments are missing in $jvmCompilerArguments: ${predefinedJvmArgumentNames - jvmArgumentNames}"
        )
    }

    @Test
    fun checkJsPredefinedCompilerArgumentsExist() {
        val jsArgumentNames = kotlinTargetCompilerArgumentsByName[jsArguments]?.map { it.name } ?: emptyList()
        val predefinedJsFirstPhaseArgumentNames = compilerArgumentsUtil.PREDEFINED_JS_FIRST_PHASE_ARGUMENTS.keys
        assertTrue(
            jsArgumentNames.containsAll(predefinedJsFirstPhaseArgumentNames),
            "Some predefined first phase arguments are missing in $jsArguments: ${predefinedJsFirstPhaseArgumentNames - jsArguments}"
        )

        val predefinedJsSecondPhaseArgumentNames = compilerArgumentsUtil.PREDEFINED_JS_SECOND_PHASE_ARGUMENTS.keys
        assertTrue(
            jsArgumentNames.containsAll(predefinedJsSecondPhaseArgumentNames),
            "Some predefined second phase arguments are missing in $jsArguments: ${predefinedJsSecondPhaseArgumentNames - jsArguments}"
        )
    }

    @Test
    fun checkWasmPredefinedCompilerArgumentsExist() {
        val wasmArgumentNames = kotlinTargetCompilerArgumentsByName[wasmArguments]?.map { it.name } ?: emptyList()
        val predefinedWasmFirstPhaseArgumentNames = compilerArgumentsUtil.PREDEFINED_WASM_FIRST_PHASE_ARGUMENTS.keys
        assertTrue(
            wasmArgumentNames.containsAll(predefinedWasmFirstPhaseArgumentNames),
            "Some predefined first phase arguments are missing in $wasmArguments: ${predefinedWasmFirstPhaseArgumentNames - jsArguments}"
        )

        val predefinedWasmSecondPhaseArgumentNames = compilerArgumentsUtil.PREDEFINED_WASM_SECOND_PHASE_ARGUMENTS.keys
        assertTrue(
            wasmArgumentNames.containsAll(predefinedWasmSecondPhaseArgumentNames),
            "Some predefined second phase arguments are missing in $wasmArguments: ${predefinedWasmSecondPhaseArgumentNames - jsArguments}"
        )
    }

    @Test
    fun checkComposeWasmPredefinedCompilerArgumentsExist() {
        val composeWasmArgumentNames =
            kotlinTargetCompilerArgumentsByName[wasmArguments]?.map { it.name } ?: emptyList()
        val predefinedComposeWasmFirstPhaseArgumentNames =
            compilerArgumentsUtil.PREDEFINED_COMPOSE_WASM_FIRST_PHASE_ARGUMENTS.keys
        assertTrue(
            composeWasmArgumentNames.containsAll(predefinedComposeWasmFirstPhaseArgumentNames),
            "Some predefined first phase arguments are missing in compose $wasmArguments: ${predefinedComposeWasmFirstPhaseArgumentNames - jsArguments}"
        )

        val predefinedComposeWasmSecondPhaseArgumentNames =
            compilerArgumentsUtil.PREDEFINED_COMPOSE_WASM_SECOND_PHASE_ARGUMENTS.keys
        assertTrue(
            composeWasmArgumentNames.containsAll(predefinedComposeWasmSecondPhaseArgumentNames),
            "Some predefined second phase arguments are missing in compose $wasmArguments: ${predefinedComposeWasmSecondPhaseArgumentNames - jsArguments}"
        )
    }

    @Test
    fun checkJvmAllowedCompilerArgumentsExist() {
        val jvmArgumentNames = kotlinTargetCompilerArgumentsByName[jvmCompilerArguments]?.map { it.name } ?: emptyList()
        val allowedJvmArgumentNames = compilerArgumentsUtil.ALLOWED_JVM_ARGUMENTS
        assertTrue(
            jvmArgumentNames.containsAll(allowedJvmArgumentNames),
            "Some allowed arguments are missing in $jvmCompilerArguments: ${allowedJvmArgumentNames - jvmArgumentNames}"
        )
    }

    @Test
    fun checkJsAllowedCompilerArgumentsExist() {
        val jsArgumentNames = kotlinTargetCompilerArgumentsByName[jsArguments]?.map { it.name } ?: emptyList()
        val allowedJsArgumentNames = compilerArgumentsUtil.ALLOWED_JS_ARGUMENTS
        assertTrue(
            jsArgumentNames.containsAll(allowedJsArgumentNames),
            "Some allowed arguments are missing in $jsArguments: ${allowedJsArgumentNames - jsArgumentNames}"
        )
    }

    @Test
    fun checkWasmAllowedCompilerArgumentsExist() {
        val wasmArgumentNames = kotlinTargetCompilerArgumentsByName[wasmArguments]?.map { it.name } ?: emptyList()
        val allowedWasmArgumentNames = compilerArgumentsUtil.ALLOWED_WASM_ARGUMENTS
        assertTrue(
            wasmArgumentNames.containsAll(allowedWasmArgumentNames),
            "Some allowed arguments are missing in $wasmArguments: ${allowedWasmArgumentNames - wasmArgumentNames}"
        )
    }
}