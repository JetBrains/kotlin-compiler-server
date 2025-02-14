rootProject.name = "kotlin-compiler-server"
pluginManagement {
    includeBuild("build-settings-logic")
}
plugins {
    id("kotlin-compiler-server-version-catalog")
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    id("kotlin-compiler-server-build-scan")
}

include(":executors")
include(":indexation")
include(":common")
include(":dependencies")