import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

group = "com.compiler.server"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

object BuildProps {
    val kotlinBuildType = "Kotlin_1320_CompilerAllPlugins"
    val kotlinBuild = "1.3.20-release-116"
    val pluginBuild = "1.3.20-release-IJ2018.3-1"
    val stdlibVersion = "1.3.20"
    val version = "1.3.20"
    val kotlinId = "1907319"
    val kotlinPluginLocation = "$kotlinBuildType/$kotlinId:id/kotlin-plugin-$pluginBuild.zip!/Kotlin/lib/kotlin-plugin.jar"
   // https://teamcity.jetbrains.com/repository/download/Kotlin_1320_CompilerAllPlugins/1907319:id/kotlin-plugin-1.3.20-release-IJ2018.3-1.zip!/Kotlin/lib/kotlin-plugin.jar
}

val kotlinDependency by configurations.creating

val copyDependencies by tasks.creating(Copy::class) {
    from(kotlinDependency)
    into("lib")
}

plugins {
    id("org.springframework.boot") version "2.2.0.RELEASE"
    id("io.spring.dependency-management") version "1.0.8.RELEASE"
    kotlin("jvm") version "1.3.21"
    kotlin("plugin.spring") version "1.3.50"
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
}

dependencies {

    kotlinDependency(kotlin("stdlib-jdk8"))
    kotlinDependency(kotlin("reflect"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    // compile
    with(BuildProps) {
        compile("org.jetbrains.intellij.deps:trove4j:1.0.20181211")
        compile("org.jetbrains.kotlin:kotlin-reflect:$version")
        compile("org.jetbrains.kotlin:kotlin-stdlib:$version")
        compile("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$stdlibVersion")
        compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$stdlibVersion")
        compile("org.jetbrains.kotlin:kotlin-test:$version")
        compile("org.jetbrains.kotlin:kotlin-compiler:$version")
        compile("org.jetbrains.kotlin:kotlin-script-runtime:$version")
        compile(dependencyFrom("https://teamcity.jetbrains.com/guestAuth/repository/download/$kotlinPluginLocation",
                artifact = "kotlin-plugin",
                version = "1.3.20")
        )
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    finalizedBy(copyDependencies)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xskip-metadata-version-check")
        jvmTarget = "1.8"
    }
}

fun dependencyFrom(
        url: String,
        artifact: String,
        version: String
) = File("$buildDir/download/$artifact-$version.jar").let { file ->
    file.parentFile.mkdirs()
    file.writeBytes(URL(url).readBytes())
    files(file.absolutePath)
}
