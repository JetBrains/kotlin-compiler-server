import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    id("base-spring-boot-conventions")
}

version = "${libs.versions.kotlin.get()}-SNAPSHOT"

dependencies {
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.docker.compose)
    implementation(libs.springdoc.webflux)
    implementation(libs.org.eclipse.lsp4j)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.rector.test)
}

tasks.named<BootBuildImage>("bootBuildImage") {
    // TODO(KTL-3803):push docker image to JB registry
    val baseImageName = "sfuri/kotlin-compiler-server-completions-lsp"
    // publish = true
    imageName = "$baseImageName:${project.version}"
    tags = setOf("$baseImageName:latest")
}
