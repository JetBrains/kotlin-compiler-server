rootProject.name = "kotlin-compiler-server"
include(":executors")
include(":indexation")
include(":common")

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
    maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
  }
}