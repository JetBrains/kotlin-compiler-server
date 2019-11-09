import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

group = "com.compiler.server"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

object BuildProps {
    val kotlinBuildType = "Kotlin_1350_Aggregate"
    val kotlinBuild = "1.3.50-release-112"
    val pluginBuild = "1.3.50-release-IJ2019.2-1"
    val version = "1.3.50"
    val kotlinId = "2491366"
    val kotlinPluginLocation = "$kotlinBuildType/$kotlinId:id/kotlin-plugin-$pluginBuild.zip!/Kotlin/lib/kotlin-plugin.jar"
}

val kotlinDependency by configurations.creating
val kotlinJsDependency by configurations.creating

val copyDependencies by tasks.creating(Copy::class) {
    from(kotlinDependency)
    into("lib")
}
val copyJSDependencies by tasks.creating(Copy::class) {
    from(files(Callable { kotlinJsDependency.map {zipTree(it)} }))
    into("js")
}

plugins {
    id("org.springframework.boot") version "2.2.0.RELEASE"
    id("io.spring.dependency-management") version "1.0.8.RELEASE"
    kotlin("jvm") version "1.3.50"
    kotlin("plugin.spring") version "1.3.50"
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
}

dependencies {

    kotlinDependency(kotlin("stdlib-jdk8"))
    kotlinDependency(kotlin("reflect"))
    kotlinJsDependency(kotlin("stdlib-js"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    with(BuildProps) {
        compile("org.jetbrains.intellij.deps:trove4j:1.0.20181211")
        compile("org.jetbrains.kotlin:kotlin-reflect:$version")
        compile("org.jetbrains.kotlin:kotlin-stdlib:$version")
        compile("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$version")
        compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version")
        compile("org.jetbrains.kotlin:kotlin-test:$version")
        compile("org.jetbrains.kotlin:kotlin-compiler:$version")
        compile("org.jetbrains.kotlin:kotlin-script-runtime:$version")
        compile("org.jetbrains.kotlin:kotlin-stdlib-js:$version")
        compile(
          dependencyFrom(
                url = "https://teamcity.jetbrains.com/guestAuth/repository/download/$kotlinPluginLocation",
                artifact = "kotlin-plugin",
                version = version
          )
        )
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xskip-metadata-version-check")
        jvmTarget = "1.8"
    }
    dependsOn(copyDependencies)
    dependsOn(copyJSDependencies)
}

tasks.withType<Test> {
    useJUnitPlatform()
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
