rootProject.name = "kotlin-compiler-server"
pluginManagement {
    includeBuild("build-settings-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
    }
}
plugins {
    id("kotlin-compiler-server-version-catalog")
}

include(":executors")
include(":indexation")
include(":common")
include(":dependencies")