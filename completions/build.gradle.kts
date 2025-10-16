
plugins {
    id("base-kotlin-jvm-conventions")
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.plugin.spring)
}

dependencies {
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.org.eclipse.lsp4j)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.kotlinx.serialization.core.jvm)
    implementation(libs.kotlinx.serialization.json.jvm)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.rector.test)
    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}

tasks.test {
    useJUnitPlatform()
}