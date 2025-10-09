package com.compiler.server.configuration

import org.springframework.context.annotation.Configuration

@Configuration
class BuildToolsConfig {
    init {
        System.setProperty("org.jetbrains.kotlin.buildtools.logger.extendedLocation", "true")
    }
}