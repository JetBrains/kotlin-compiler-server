plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":common", configuration = "default"))
}

application {
    mainClass.set("cache.MainKt")
}

tasks.withType<JavaExec> {
    dependsOn(":dependencies:copyDependencies")
    dependsOn(":dependencies:copyWasmDependencies")
    dependsOn(":dependencies:copyComposeWasmCompilerPlugins")
    dependsOn(":dependencies:copyComposeWasmDependencies")

    val rootName = project.rootProject.projectDir.toString()

    val kotlinVersion = libs.versions.kotlin.get()
    inputs.property("kotlinVersion", kotlinVersion)

    // Adding classpath directories as task input for up-to-date checks
    inputs.dir(libWasmFolder)
    inputs.dir(libComposeWasmFolder)
    inputs.dir(libComposeWasmCompilerPluginsFolder)

    // Adding resulting index files as output for up-to-date checks
    val composeCacheComposeWasm = "$rootName${File.separator}$cachesComposeWasm"
    outputs.dir(cachesComposeWasmFolder)

    args = listOf(
        kotlinVersion,
        libJVMFolder.asFile.absolutePath,
        composeCacheComposeWasm,
    )
}
