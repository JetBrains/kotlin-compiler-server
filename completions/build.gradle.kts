import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    id("base-spring-boot-conventions")
}

version = "${libs.versions.kotlin.get()}-SNAPSHOT"

val depsProject = project(":dependencies")

dependencies {
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.docker.compose)
    implementation(libs.springdoc.webflux)
    implementation(libs.org.eclipse.lsp4j)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.jackson.module.kotlin)
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

    val compilerPlugins = depsProject.configurations["kotlinCompilerPluginDependency"]
        .resolvedConfiguration
        .resolvedArtifacts
        .map { "id(\"${it.moduleVersion}\")" }

    val deps = depsProject.configurations["kotlinDependency"]
        .resolvedConfiguration
        .resolvedArtifacts
        .map { "implementation(\"${it.moduleVersion}\")" }

    val kotlinVersionPlaceholder = "{{kotlin_version}}"
    val pluginsPlaceholder = "{{plugins}}"
    val dependenciesPlaceholder = "{{dependencies}}"

    val newContent: String = listOf(kotlinVersionPlaceholder, pluginsPlaceholder, dependenciesPlaceholder)
        .zip(listOf(setOf(kotlinVersion), compilerPlugins, deps))
        .fold(buildFile.readText()) { acc, (placeholder, values) ->
            acc.replace(placeholder, values.joinToString("\n    "))
        }
    buildFile.writeText(newContent)
    println("LSP workspace root for kotlin=$kotlinVersion created at: ${targetDir.path}")
}

tasks.processResources {
    val kotlinVersionProvider = providers.provider { kotlinVersion }

    filesMatching("**/application*.yml") {
        filter { line -> line.replace("@@KOTLIN_VERSION@@", kotlinVersionProvider.get()) }
    }
}

tasks.withType<Test> {

    // We disable this on TeamCity, because we don't want to fail this test,
    // when compiler server's test run as a K2 user project.
    // But for our pull requests we still need to run this test, so we add it to our GitHub action.
    if (System.getenv("TEAMCITY_VERSION") != null) {
        this.enabled = false
    }
}
