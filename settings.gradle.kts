rootProject.name = "kotlin-compiler-server"
include(":executors")
include(":indexation")
include(":common")

pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
        maven("https://plugins.gradle.org/m2/")
        mavenCentral()
    }
}