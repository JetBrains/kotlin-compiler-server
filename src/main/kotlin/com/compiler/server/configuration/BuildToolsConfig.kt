package com.compiler.server.configuration

import org.springframework.context.annotation.Configuration

@Configuration
class BuildToolsConfig {
    init {
        /**
         * This flag is used by KotlinMessageRenderer in kotlin-build-tools-api to properly format
         * returned log messages during compilation. When this flag is set, the whole position of
         * a warning/error is returned instead of only the beginning. We need this behavior to
         * process messages in KotlinLogger and then correctly mark errors on the frontend.
         *
         * Setting this property should be removed after KT-80963 is implemented, as KotlinLogger
         * will return the full position of an error by default then.
         */
        System.setProperty("org.jetbrains.kotlin.buildtools.logger.extendedLocation", "true")
    }
}