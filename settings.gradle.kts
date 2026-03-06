rootProject.name = "kotlin-compiler-server"
pluginManagement {
    includeBuild("build-settings-logic")
}
plugins {
    id("kotlin-compiler-server-version-catalog")
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("kotlin-compiler-server-build-scan")
}

include(":executors")
include(":common")
include(":dependencies")
include(":completions")
include(":cache-maker")
include(":resource-server")