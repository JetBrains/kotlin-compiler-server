
val kotlinVersion: String by System.getProperties()
val indexes: String by System.getProperties()
val indexesJs: String by System.getProperties()

plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":common", configuration = "default"))
    implementation("junit:junit:4.12")
    implementation("org.jetbrains.intellij.deps:trove4j:1.0.20200330")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")
    implementation("org.jetbrains.kotlin:ide-common-ij201:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-plugin-ij201:$kotlinVersion") {
        isTransitive = false
    }

    testImplementation("junit:junit:4.12")
}

application {
    mainClass.set("indexation.MainKt")
}

tasks.withType<JavaExec> {
    val rootName = project.rootProject.projectDir.toString()
    args = listOf("$rootName${File.separator}$kotlinVersion", "$rootName${File.separator}$indexes", "$rootName${File.separator}$indexesJs")
}
