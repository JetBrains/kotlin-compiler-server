package com.compiler.server.compiler.components

import com.compiler.server.model.bean.LibrariesFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class KotlinEnvironmentConfiguration(val librariesFile: LibrariesFile) {
    @Bean
    fun kotlinEnvironment(): KotlinEnvironment {
        val classPath =
                listOfNotNull(librariesFile.jvm)
                        .flatMap {
                            it.listFiles()?.toList()
                                    ?: throw error("No kotlin libraries found in: ${librariesFile.jvm.absolutePath}")
                        }

        val additionalJsClasspath = listOfNotNull(librariesFile.js)
        return KotlinEnvironment(classPath, additionalJsClasspath)
    }
}

class KotlinEnvironment(
        val classpath: List<File>,
        private val additionalJsClaspath: List<File>
) {
    companion object {
        /**
         * This list allows to configure behavior of webdemo compiler. Its effect is equivalent
         * to passing this list of string to CLI compiler.
         *
         * See [org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments] and
         * [org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments] for list of possible flags
         */
        private val additionalCompilerArguments: List<String> = listOf(
                "-Xuse-experimental=kotlin.ExperimentalStdlibApi",
                "-Xuse-experimental=kotlin.time.ExperimentalTime",
                "-Xuse-experimental=kotlin.Experimental",
                "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes",
                "-Xuse-experimental=kotlin.contracts.ExperimentalContracts",
                "-Xuse-experimental=kotlin.experimental.ExperimentalTypeInference",
                "-XXLanguage:+InlineClasses"
        )
    }

    fun <T> environment(f: (KotlinCoreEnvironment) -> T): T {
        val disposable = Disposer.newDisposable()
        val coreEnvironment = createCoreEnvironment(disposable)
        try {
            return f(coreEnvironment)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private val configuration = createConfiguration()

    @Synchronized
    private fun createCoreEnvironment(disposable: Disposable): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForProduction(
                parentDisposable = disposable,
                configuration = configuration.copy(),
                configFiles = EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }

    private fun createConfiguration(): CompilerConfiguration {
        val arguments = K2JVMCompilerArguments()
        parseCommandLineArguments(additionalCompilerArguments, arguments)
        return CompilerConfiguration().apply {
            addJvmClasspathRoots(classpath.filter { it.exists() && it.isFile && it.extension == "jar" })
            val messageCollector = MessageCollector.NONE
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            put(CommonConfigurationKeys.MODULE_NAME, "web-module")
            with(K2JVMCompilerArguments()) {
                put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, noParamAssertions)
                put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, noCallAssertions)
                put(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, true)
            }
            languageVersionSettings = arguments.toLanguageVersionSettings(messageCollector)
        }
    }

    fun createJsEnvironment(coreEnvironment: KotlinCoreEnvironment): CompilerConfiguration {
        return coreEnvironment.configuration.copy().apply {
            put(CommonConfigurationKeys.MODULE_NAME, "moduleId")
            put(JSConfigurationKeys.LIBRARIES, additionalJsClaspath.map { it.absolutePath })
        }
    }
}
