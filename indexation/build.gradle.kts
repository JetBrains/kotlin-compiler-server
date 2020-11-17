
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
    testImplementation("junit:junit:4.12")
//    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.10.0")
    testImplementation("com.fasterxml.jackson.core:jackson-core:2.10.0")
    testImplementation("com.fasterxml.jackson.core:jackson-annotations:2.10.0")
    // Kotlin libraries
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.4.1")
}

application {
    mainClass.set("indexation.MainKt")
}

tasks.withType<JavaExec> {
    val rootName = project.rootProject.projectDir.toString()
    args = listOf("$rootName${File.separator}$kotlinVersion", "$rootName${File.separator}$indexes")
}
