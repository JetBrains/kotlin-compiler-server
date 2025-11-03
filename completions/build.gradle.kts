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

tasks.withType<Test> {

    // We disable this on TeamCity, because we don't want to fail this test,
    // when compiler server's test run as a K2 user project.
    // But for our pull requests we still need to run this test, so we add it to our GitHub action.
    if (System.getenv("TEAMCITY_VERSION") != null) {
        this.enabled = false
    }
}