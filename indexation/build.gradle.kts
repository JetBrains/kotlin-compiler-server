
val kotlinVersion: String by System.getProperties()
val kotlinIdeVersion: String by System.getProperties()
val indexes: String by System.getProperties()

plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":common", configuration = "default"))
    implementation("org.jetbrains.kotlin:idea:202-$kotlinIdeVersion-IJ8194.7") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:kotlin-compiler-for-ide:$kotlinVersion")
}

application {
    mainClassName = "indexation.MainKt"
}

tasks.withType<JavaExec> {
    val rootName = project.rootProject.projectDir.toString()
    args = listOf("$rootName${File.separator}$kotlinVersion", "$rootName${File.separator}$indexes")
}
