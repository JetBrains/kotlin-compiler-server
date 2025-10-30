import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    id("base-spring-boot-conventions")
}

val kotlinVersion = "2.2.20" // libs.versions.kotlin.get()
version = "$kotlinVersion-SNAPSHOT"

val depsProject = project(":dependencies")

dependencies {
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.docker.compose)
    implementation(libs.springdoc.webflux)
    implementation(libs.org.eclipse.lsp4j)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.rector.test)
    implementation(depsProject)
}

tasks.named<BootBuildImage>("bootBuildImage") {
    // TODO(KTL-3803):push docker image to JB registry
    val baseImageName = "sfuri/kotlin-compiler-server-completions-lsp"
    // publish = true
    imageName = "$baseImageName:${project.version}"
    tags = setOf("$baseImageName:latest")
}

tasks.register("generateLspWorkspaceRoot") {
    dependsOn(":dependencies:jar")

    val (rootDir, targetDir) = layout.projectDirectory.dir("src/main/resources/lsp/workspaces").let {
        it.dir("root-template").asFile to it.dir("lsp-workspace-root-${kotlinVersion}").asFile
    }
    targetDir.mkdirs()
    rootDir.copyRecursively(targetDir, overwrite = true)

    val buildFile = targetDir.resolve("build.gradle.kts")

    // TODO(Stefano Furi): retrieve "plugin.serialization" name not "org.jetbrains...", or how do we use the latter
    val compilerPlugins = /* depsProject.configurations["kotlinCompilerPluginDependency"]
        ?.resolvedConfiguration
        ?.resolvedArtifacts
        ?.map { "kotlin(\"${it.moduleVersion.id.name}\") version \"${it.moduleVersion.id.version}\"" }
        ?: emptySet() */ emptySet<String>()

    val deps = depsProject.configurations["kotlinDependency"]
        ?.resolvedConfiguration
        ?.resolvedArtifacts
        ?.map { "implementation(\"${it.moduleVersion}\")" }
        ?: emptySet()

    val kotlinVersionPlaceholder = "{{kotlin_version}}"
    val pluginsPlaceholder = "{{plugins}}"
    val dependenciesPlaceholder = "{{dependencies}}"

    val newContent: String = listOf(kotlinVersionPlaceholder, pluginsPlaceholder, dependenciesPlaceholder)
        .zip(listOf(setOf(kotlinVersion), compilerPlugins, deps))
        .fold(buildFile.readText()) { acc, (placeholder, values) ->
            acc.replace(placeholder, values.joinToString("\n    "))
        }
    buildFile.writeText(newContent)
    println("Generated LSP workspace root for kotlin=$kotlinVersion at: ${targetDir.path}")
}

tasks.processResources {
    val kotlinVersionProvider = providers.provider { kotlinVersion }

    filesMatching("**/application*.yml") {
        filter { line -> line.replace("@@KOTLIN_VERSION@@", kotlinVersionProvider.get()) }
    }
}
