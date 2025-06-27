plugins {
    id("base-kotlin-jvm-conventions")
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
    dependsOn(":executors:jar")
    dependsOn(":dependencies:copyDependencies")
    dependsOn(":dependencies:copyCompilerPluginDependencies")
    dependsOn(":dependencies:copyJSDependencies")
    dependsOn(":dependencies:copyWasmDependencies")
    dependsOn(":dependencies:copyComposeWasmCompilerPlugins")
    dependsOn(":dependencies:copyComposeWasmDependencies")

    val rootName = project.rootProject.projectDir.toString()

    val kotlinVersion = libs.versions.kotlin.get()
    inputs.property("kotlinVersion", kotlinVersion)

    // Adding classpath directories as task input for up-to-date checks
    inputs.dir(libJVMFolder)
    inputs.dir(compilerPluginsForJVMFolder)
    inputs.dir(libJSFolder)
    inputs.dir(libWasmFolder)
    inputs.dir(libComposeWasmFolder)
    inputs.dir(libComposeWasmCompilerPluginsFolder)

    // Adding resulting index files as output for up-to-date checks
    val jvmIndicesJson = "$rootName${File.separator}$indexes"
    val jsIndicesJson = "$rootName${File.separator}$indexesJs"
    val wasmIndicesJson = "$rootName${File.separator}$indexesWasm"
    val composeWasmIndicesJson = "$rootName${File.separator}$indexesComposeWasm"
    outputs.files(jvmIndicesJson, jsIndicesJson, wasmIndicesJson, composeWasmIndicesJson)

    args = listOf(
        kotlinVersion,
        libJVMFolder.asFile.absolutePath,
        jvmIndicesJson,
        jsIndicesJson,
        wasmIndicesJson,
        composeWasmIndicesJson,
    )
}
