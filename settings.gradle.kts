pluginManagement {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    }
    plugins{
        kotlin("jvm") version "1.5.20-282"
        kotlin("plugin.spring") version "1.5.20-282"
    }
}

rootProject.name = "kotlin-compiler-server"
include(":executors")
include(":indexation")
include(":common")
