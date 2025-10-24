import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    id("base-spring-boot-conventions")
}

version = "${libs.versions.kotlin.get()}-SNAPSHOT"

dependencies {
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.org.eclipse.lsp4j)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.rector.test)
}

tasks.named<BootBuildImage>("bootBuildImage") {
    val baseImageName = "sfuri/kotlin-compiler-server-completions-lsp"
    // TODO: push docker image to JB registry
    // publish = true
    imageName = "$baseImageName:${project.version}"
    tags = setOf("$baseImageName:latest")
}
