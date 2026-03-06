package com.compiler.server.compiler.components

import com.compiler.server.common.components.CompilerPluginOption
import com.compiler.server.common.components.KotlinEnvironment
import com.compiler.server.model.bean.LibrariesFile
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(value = [DependenciesProperties::class])
class KotlinEnvironmentConfiguration(
    val librariesFile: LibrariesFile,
    val dependenciesProperties: DependenciesProperties,
) {
    @Bean
    fun kotlinEnvironment(): KotlinEnvironment {
        val classPath =
            listOfNotNull(librariesFile.jvm)
                .flatMap {
                    it.listFiles()?.toList()
                        ?: error("No kotlin libraries found in: ${librariesFile.jvm.absolutePath}")
                }

        val additionalJsClasspath = librariesFile.js.listFiles()?.toList() ?: emptyList()
        val additionalWasmClasspath = librariesFile.wasm.listFiles()?.toList() ?: emptyList()
        val additionalComposeWasmClasspath = librariesFile.composeWasm.listFiles()?.toList() ?: emptyList()
        val composeWasmCompilerPlugins = librariesFile.composeWasmComposeCompiler.listFiles()?.toList() ?: emptyList()
        val compilerPlugins = librariesFile.compilerPlugins.listFiles()?.toList() ?: emptyList()

        return KotlinEnvironment(
            classPath,
            additionalJsClasspath,
            additionalWasmClasspath,
            additionalComposeWasmClasspath,
            composeWasmCompilerPlugins,
            compilerPlugins,
            listOf(
                CompilerPluginOption(
                    "androidx.compose.compiler.plugins.kotlin",
                    "generateDecoys",
                    "false"
                ),
            ),
            dependenciesProperties.composeWasm,
            dependenciesProperties.staticUrl,
        )
    }
}

@ConfigurationProperties(prefix = "dependencies")
class DependenciesProperties {
    lateinit var composeWasm: String
    lateinit var staticUrl: String
}
