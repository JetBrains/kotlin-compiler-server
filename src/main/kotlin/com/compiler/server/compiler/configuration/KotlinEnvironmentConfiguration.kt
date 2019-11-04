package com.compiler.server.compiler.configuration

import com.compiler.server.compiler.components.KotlinEnvironment
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class KotlinEnvironmentConfiguration {

    @Bean
    fun kotlinEnvironment() = KotlinEnvironment
            .with(classpath = listOfNotNull(File("lib"))
                    .flatMap { it.listFiles().toList() })
}