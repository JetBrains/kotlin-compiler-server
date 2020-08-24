
val kotlinVersion: String by System.getProperties()
val indexes: String by System.getProperties()

plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":common", configuration = "default"))
    implementation("org.jetbrains.kotlin:kotlin-plugin-ij193:$kotlinVersion") {
        isTransitive = false
    }
}

application {
    mainClassName = "indexation.Main"
}

tasks.withType<JavaExec> {
    val rootName = project.rootProject.projectDir.toString()
    args = listOf("$rootName${File.separator}$kotlinVersion", "$rootName${File.separator}$indexes")
}
