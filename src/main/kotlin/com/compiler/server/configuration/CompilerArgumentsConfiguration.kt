package com.compiler.server.configuration

import com.compiler.server.model.ExtendedCompilerArgument
import com.compiler.server.model.ProjectType
import com.compiler.server.utils.CompilerArgumentsUtil
import com.compiler.server.validation.AbstractCompilerArgumentsValidator
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.utils.keysToMap
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CompilerArgumentsConfiguration {

    @Bean
    fun jvmCompilerArguments(
        kotlinTargetCompilerArgumentsByName: Map<String, Set<KotlinCompilerArgument>>,
        compilerArgumentsUtil: CompilerArgumentsUtil
    ): Set<ExtendedCompilerArgument> {
        return compilerArgumentsUtil.collectJvmArguments(kotlinTargetCompilerArgumentsByName)
    }

    @Bean
    fun jsCompilerArguments(
        kotlinTargetCompilerArgumentsByName: Map<String, Set<KotlinCompilerArgument>>,
        compilerArgumentsUtil: CompilerArgumentsUtil
    ): Set<ExtendedCompilerArgument> {
        return compilerArgumentsUtil.collectJsArguments(kotlinTargetCompilerArgumentsByName)
    }

    @Bean
    fun wasmCompilerArguments(
        kotlinTargetCompilerArgumentsByName: Map<String, Set<KotlinCompilerArgument>>,
        compilerArgumentsUtil: CompilerArgumentsUtil
    ): Set<ExtendedCompilerArgument> {
        return compilerArgumentsUtil.collectWasmArguments(kotlinTargetCompilerArgumentsByName)
    }

    @Bean
    fun composeWasmCompilerArguments(
        kotlinTargetCompilerArgumentsByName: Map<String, Set<KotlinCompilerArgument>>,
        compilerArgumentsUtil: CompilerArgumentsUtil
    ): Set<ExtendedCompilerArgument> {
        return compilerArgumentsUtil.collectComposeWasmArguments(kotlinTargetCompilerArgumentsByName)
    }

    @Bean
    fun compilerArgumentsValidators(
        jvmCompilerArgumentsValidator: AbstractCompilerArgumentsValidator,
        jsCompilerArgumentsValidator: AbstractCompilerArgumentsValidator,
        wasmCompilerArgumentsValidator: AbstractCompilerArgumentsValidator,
        composeWasmCompilerArgumentsValidator: AbstractCompilerArgumentsValidator
    ): Map<ProjectType, AbstractCompilerArgumentsValidator> {
        return ProjectType.entries.keysToMap {
            when (it) {
                ProjectType.JAVA, ProjectType.JUNIT -> jvmCompilerArgumentsValidator
                ProjectType.JS, ProjectType.JS_IR, ProjectType.CANVAS -> jsCompilerArgumentsValidator
                ProjectType.WASM -> wasmCompilerArgumentsValidator
                ProjectType.COMPOSE_WASM -> composeWasmCompilerArgumentsValidator
            }
        }
    }


    @Bean
    fun kotlinTargetCompilerArgumentsByName(): Map<String, Set<KotlinCompilerArgument>> {
        val rootLevel = kotlinCompilerArguments.topLevel

        val compilerArgumentsByTarget = mutableMapOf<String, Set<KotlinCompilerArgument>>()

        val stack = mutableListOf<Pair<KotlinCompilerArgumentsLevel, List<KotlinCompilerArgument>>>()
        stack.add(rootLevel to emptyList())

        while (stack.isNotEmpty()) {
            val (currentLevel, parentArguments) = stack.removeAt(stack.size - 1)

            val currentArguments = parentArguments + currentLevel.arguments

            if (currentLevel.nestedLevels.isEmpty()) {
                compilerArgumentsByTarget[currentLevel.name] = currentArguments.toSet()
            } else {
                for (nestedLevel in currentLevel.nestedLevels) {
                    stack.add(nestedLevel to currentArguments)
                }
            }
        }
        return compilerArgumentsByTarget
    }
}