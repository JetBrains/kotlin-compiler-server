package com.compiler.server.configuration

import com.compiler.server.model.ExtendedCompilerArgument
import com.compiler.server.model.ProjectType
import com.compiler.server.utils.COMPILER_ARGUMENTS_JSON
import com.compiler.server.utils.CompilerArgumentsUtil
import com.compiler.server.validation.AbstractCompilerArgumentsValidator
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArguments
import org.jetbrains.kotlin.utils.keysToMap
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CompilerArgumentsConfiguration {

    @Bean
    fun kotlinCompilerArguments() = collectCompilerArguments()

    @Bean
    fun jvmCompilerArguments(
        kotlinCompilerArguments: KotlinCompilerArguments,
        compilerArgumentsUtil: CompilerArgumentsUtil
    ): Set<ExtendedCompilerArgument> {
        return compilerArgumentsUtil.collectJvmArguments(kotlinCompilerArguments)
    }

    @Bean
    fun jsCompilerArguments(
        kotlinCompilerArguments: KotlinCompilerArguments,
        compilerArgumentsUtil: CompilerArgumentsUtil
    ): Set<ExtendedCompilerArgument> {
        return compilerArgumentsUtil.collectJsArguments(kotlinCompilerArguments)
    }

    @Bean
    fun wasmCompilerArguments(
        kotlinCompilerArguments: KotlinCompilerArguments,
        compilerArgumentsUtil: CompilerArgumentsUtil
    ): Set<ExtendedCompilerArgument> {
        return compilerArgumentsUtil.collectWasmArguments(kotlinCompilerArguments)
    }

    @Bean
    fun composeWasmCompilerArguments(
        kotlinCompilerArguments: KotlinCompilerArguments,
        compilerArgumentsUtil: CompilerArgumentsUtil
    ): Set<ExtendedCompilerArgument> {
        return compilerArgumentsUtil.collectComposeWasmArguments(kotlinCompilerArguments)
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

    private fun collectCompilerArguments(): KotlinCompilerArguments {
        val jsonConverter = Json {
            prettyPrint = true
            encodeDefaults = true
        }

        val compilerArgumentsJsonString =
            KotlinCompilerArguments::class.java.classLoader
                .getResource(COMPILER_ARGUMENTS_JSON)?.readText()
                ?: error("Can't find $COMPILER_ARGUMENTS_JSON in the classpath")

        return jsonConverter.decodeFromString<KotlinCompilerArguments>(compilerArgumentsJsonString)
    }
}