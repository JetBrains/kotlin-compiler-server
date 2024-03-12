plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":common", configuration = "default"))
    implementation("org.jetbrains.kotlin:kotlin-compiler-for-ide:$kotlinIdeVersion") {
        isTransitive = false
    }
}

application {
    mainClass.set("indexation.MainKt")
}

tasks.withType<JavaExec> {
    val rootName = project.rootProject.projectDir.toString()
    args = listOf(
        kotlinVersion,
        "$rootName${File.separator}$kotlinVersion",
        "$rootName${File.separator}$indexes",
        "$rootName${File.separator}$indexesJs",
        "$rootName${File.separator}$indexesWasm",
        "$rootName${File.separator}$indexesComposeWasm",
    )
}
