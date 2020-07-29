
val kotlinVersion: String by System.getProperties()

plugins {
    kotlin("jvm")
    application
}

application {
    mainClassName = "indexation.Main"
}

tasks.withType<JavaExec> {
    val rootName = project.rootProject.projectDir.toString()
    args = listOf("$rootName/$kotlinVersion", "$rootName/indexes.json")
}