/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Provider


val buildingOnTeamCity: Boolean = System.getenv("TEAMCITY_VERSION") != null
val buildingOnGitHub: Boolean = System.getenv("GITHUB_ACTION") != null
val buildingOnCi: Boolean = System.getenv("CI") != null || buildingOnTeamCity || buildingOnGitHub

// NOTE: build scan properties are documented in README.md
val Settings.buildScanEnabled: Provider<Boolean>
    get() =
        kotlinCompilerServerProperty("build.scan.enabled", String::toBoolean)
            .orElse(buildingOnCi)

internal const val DEFAULT_KOTLIN_COMPILER_SERVER_USER_NAME = "<default>"

/**
 * Optionaly override the default name attached to a Build Scan.
 */
val Settings.buildScanUsername: Provider<String>
    get() =
        kotlinCompilerServerProperty("build.scan.username")
            .orElse(DEFAULT_KOTLIN_COMPILER_SERVER_USER_NAME)
            .map(String::trim)

internal fun Settings.kotlinCompilerServerProperty(name: String): Provider<String> =
    providers.gradleProperty("org.jetbrains.kotlin.compiler.server.$name")

internal fun <T : Any> Settings.kotlinCompilerServerProperty(name: String, convert: (String) -> T): Provider<T> =
    kotlinCompilerServerProperty(name).map(convert)

