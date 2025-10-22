plugins {
    id("base-spring-boot-conventions")
}

dependencies {
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.org.eclipse.lsp4j)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.rector.test)
}