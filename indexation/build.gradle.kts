
val kotlinVersion: String by System.getProperties()

plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation("com.google.code.gson:gson:2.8.6")
}

application {
    mainClassName = "indexation.Main"
}

tasks.withType<JavaExec> {
    val rootName = project.rootProject.projectDir.toString()
    args = listOf("$rootName/$kotlinVersion", "$rootName/indexes.json")
}