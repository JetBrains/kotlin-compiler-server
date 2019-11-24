import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

object BuildProps {
    private const val kotlinBuildType = "Kotlin_KotlinRelease_1360_Aggregate"
    private const val pluginBuild = "1.3.60-release-IJ2019.3-1"
    private const val kotlinId = "58008784"
    const val version = "1.3.60"
    const val kotlinPluginLocation = "$kotlinBuildType/$kotlinId:id/kotlin-plugin-$pluginBuild.zip!/Kotlin/lib/kotlin-plugin.jar"
}

group = "com.compiler.server"
version = "${BuildProps.version}-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

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
    kotlin("jvm") version "1.3.60"
    kotlin("plugin.spring") version "1.3.50"
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
}

dependencies {

    kotlinDependency("junit:junit:4.12")
    kotlinDependency("org.hamcrest:hamcrest-core:1.3")
    kotlinDependency(kotlin("stdlib-jdk8"))
    kotlinDependency(kotlin("reflect"))
    kotlinJsDependency(kotlin("stdlib-js"))

    File("src/main/resources/application.properties").apply{
        parentFile.mkdirs()
        writeText("kotlin.version=${BuildProps.version}")
    }

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("junit:junit:4.12")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.2")
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
                url = "https://buildserver.labs.intellij.net/guestAuth/repository/download/$kotlinPluginLocation",
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
