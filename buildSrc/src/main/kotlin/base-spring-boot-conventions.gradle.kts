import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("base-kotlin-jvm-conventions")
}

// https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
val libs = the<LibrariesForLibs>()

// https://github.com/gradle/gradle/issues/17968#issuecomment-894742093
pluginManager.apply(libs.plugins.spring.boot.get().pluginId)
pluginManager.apply(libs.plugins.spring.dependency.management.get().pluginId)
pluginManager.apply(libs.plugins.kotlin.plugin.spring.get().pluginId)

dependencies {
    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
