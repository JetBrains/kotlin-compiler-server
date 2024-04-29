import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

val policy: String by System.getProperties()

group = "com.compiler.server"
version = "$kotlinVersion-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

val propertyFile = "application.properties"

plugins {
    id("org.springframework.boot") version "2.7.10"
    id("io.spring.dependency-management") version "1.1.3"
    val kotlinVersion by System.getProperties()
    kotlin("jvm") version "$kotlinVersion"
    kotlin("plugin.spring") version "$kotlinVersion"
}

apply<NodeJsRootPlugin>()

// for new Wasm opcodes
the<NodeJsRootExtension>().nodeVersion = "21.0.0-v8-canary202309167e82ab1fa2"
the<NodeJsRootExtension>().nodeDownloadBaseUrl = "https://nodejs.org/download/v8-canary"

allprojects {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.spring.io/snapshot")
        maven("https://repo.spring.io/milestone")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        maven("https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-third-party-dependencies")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
        maven("https://www.myget.org/F/rd-snapshots/maven/")
        maven("https://kotlin.jetbrains.space/p/kotlin/packages/maven/kotlin-ide")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    afterEvaluate {
        dependencies {
            dependencies {
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
                implementation("org.jetbrains.kotlin:idea:231-$kotlinIdeVersion-$kotlinIdeVersionSuffix") {
                    isTransitive = false
                }
            }
        }
    }
}

val resourceDependency: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    annotationProcessor("org.springframework:spring-context-indexer")
    implementation("com.google.code.gson:gson")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.amazonaws.serverless:aws-serverless-java-container-springboot2:1.9.3")
    implementation("junit:junit:4.13.2")
    implementation("net.logstash.logback:logstash-logback-encoder:7.3")
    implementation("org.jetbrains.intellij.deps:trove4j:1.0.20221201")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-compiler-for-ide:$kotlinIdeVersion"){
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:core:231-$kotlinIdeVersion-$kotlinIdeVersionSuffix")
    implementation(project(":executors", configuration = "default"))
    implementation(project(":common", configuration = "default"))

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    resourceDependency("org.jetbrains.skiko:skiko-js-wasm-runtime:0.7.90")
}

fun buildPropertyFile() {
    rootDir.resolve("src/main/resources/${propertyFile}").apply {
        println("Generate properties into $absolutePath")
        parentFile.mkdirs()
        writeText(generateProperties())
    }
}

fun generateProperties(prefix: String = "") = """
    # this file is autogenerated by build.gradle.kts
    kotlin.version=${kotlinVersion}
    policy.file=${prefix + policy}
    indexes.file=${prefix + indexes}
    indexesJs.file=${prefix + indexesJs}
    indexesWasm.file=${prefix + indexesWasm}
    indexesComposeWasm.file=${prefix + indexesComposeWasm}
    libraries.folder.jvm=${prefix + libJVMFolder}
    libraries.folder.js=${prefix + libJSFolder}
    libraries.folder.wasm=${prefix + libWasmFolder}
    libraries.folder.compose-wasm=${prefix + libComposeWasmFolder}
    libraries.folder.compose-wasm-compiler-plugins=${prefix + libComposeWasmCompilerPluginsFolder}
    spring.mvc.pathmatch.matching-strategy=ant_path_matcher
    server.compression.enabled=true
    server.compression.mime-types=application/json
""".trimIndent()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of("17"))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
    dependsOn(":dependencies:copyDependencies")
    dependsOn(":dependencies:copyJSDependencies")
    dependsOn(":dependencies:copyWasmDependencies")
    dependsOn(":dependencies:copyComposeWasmDependencies")
    dependsOn(":dependencies:copyComposeWasmCompilerPlugins")
    dependsOn(":executors:jar")
    dependsOn(":indexation:run")
    buildPropertyFile()
}
println("Using Kotlin compiler $kotlinVersion")

tasks.withType<BootJar> {
    requiresUnpack("**/kotlin-*.jar")
    requiresUnpack("**/kotlinx-*.jar")
}

val buildLambda by tasks.creating(Zip::class) {
    val lambdaWorkDirectoryPath = "/var/task/"
    from(tasks.compileKotlin)
    from(tasks.processResources) {
        eachFile {
            if (name == propertyFile) { file.writeText(generateProperties(lambdaWorkDirectoryPath)) }
        }
    }
    from(policy)
    from(indexes)
    from(indexesJs)
    from(indexesWasm)
    from(libJSFolder) { into(libJS) }
    from(libWasmFolder) { into(libWasm) }
    from(libComposeWasmFolder) { into(libComposeWasm) }
    from(libJVMFolder) { into(libJVM) }
    from(libComposeWasmCompilerPluginsFolder) { into(libComposeWasmCompilerPlugins) }
    into("lib") {
        from(configurations.compileClasspath) { exclude("tomcat-embed-*") }
    }
}

tasks.named<Copy>("processResources") {
    val archiveOperation = project.serviceOf<ArchiveOperations>()
    from(resourceDependency.map {
        archiveOperation.zipTree(it)
    }) {
        into("com/compiler/server")
    }
}

tasks.withType<Test> {
    dependsOn(rootProject.the<NodeJsRootExtension>().nodeJsSetupTaskProvider)
    useJUnitPlatform()
    doFirst {
        this@withType.environment(
            "kotlin.wasm.node.path",
            rootProject.the<NodeJsRootExtension>().requireConfigured().nodeExecutable
        )
    }
}
