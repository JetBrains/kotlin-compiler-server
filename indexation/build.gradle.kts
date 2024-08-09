plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":common", configuration = "default"))
    implementation(libs.kotlin.compiler.ide) {
        isTransitive = false
    }
}

application {
    mainClass.set("indexation.MainKt")
}

tasks.withType<JavaExec> {
    val rootName = project.rootProject.projectDir.toString()
    args = listOf(
        libs.versions.kotlin.get(),
        "$rootName${File.separator}${libs.versions.kotlin.get()}",
        "$rootName${File.separator}$indexes",
        "$rootName${File.separator}$indexesJs",
        "$rootName${File.separator}$indexesWasm",
        "$rootName${File.separator}$indexesComposeWasm",
    )
}
