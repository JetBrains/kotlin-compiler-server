import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

val policy: String by System.getProperties()

group = "com.compiler.server"
version = "${libs.versions.kotlin.get()}-SNAPSHOT"

val propertyFile = "application.properties"

plugins {
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.spring)
}

kotlin.jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
    vendor.set(JvmVendorSpec.AMAZON)
}

apply<NodeJsRootPlugin>()

allprojects {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.spring.io/snapshot")
        maven("https://repo.spring.io/milestone")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide")
        maven("https://packages.jetbrains.team/maven/p/kt/dev/")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        maven("https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-third-party-dependencies")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
        maven("https://www.myget.org/F/rd-snapshots/maven/")
        maven("https://kotlin.jetbrains.space/p/kotlin/packages/maven/kotlin-ide")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

setOf(
    rootProject,
    project(":common"),
    project(":executors"),
    project(":indexation"),
).forEach { project ->
    project.afterEvaluate {
        project.dependencies {
            project.dependencies {
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
                implementation(libs.kotlin.idea) {
                    isTransitive = false
                }
            }
        }
    }
}

val kotlinComposeWasmStdlibFile: Configuration by configurations.creating {
    isTransitive = false
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(
            Category.CATEGORY_ATTRIBUTE,
            objects.categoryComposeCache
        )
        attribute(
            CacheAttribute.cacheAttribute,
            CacheAttribute.WASM
        )
    }
}

val composeWasmStaticResources: Configuration by configurations.creating {
    isTransitive = false
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(
            Category.CATEGORY_ATTRIBUTE,
            objects.categoryComposeWasmResources
        )
    }
}

dependencies {
    annotationProcessor("org.springframework:spring-context-indexer")
    implementation("com.google.code.gson:gson")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(libs.springfox.boot.starter)
    implementation(libs.aws.springboot.container)
    implementation(libs.junit)
    implementation(libs.logback.logstash.encoder)
    implementation(libs.intellij.trove4j)
    implementation(libs.kotlin.reflect)
    implementation(libs.bundles.kotlin.stdlib)
    implementation(libs.kotlin.test)
    implementation(libs.kotlin.compiler)
    implementation(libs.kotlin.script.runtime)
    implementation(libs.kotlin.compiler.ide) {
        isTransitive = false
    }
    implementation(libs.kotlin.core)
    implementation(project(":executors", configuration = "default"))
    implementation(project(":common", configuration = "default"))

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation(libs.kotlinx.coroutines.test)

    kotlinComposeWasmStdlibFile(project(":cache-maker"))
    composeWasmStaticResources(project(":resource-server"))
}

fun Project.generateProperties(
    prefix: String = "",
): Map<String, String> = mapOf(
    "kotlin.version" to kotlinVersion,
    "policy.file" to prefix + policy,
    "indexes.file" to prefix + indexes,
    "indexesJs.file" to prefix + indexesJs,
    "indexesWasm.file" to prefix + indexesWasm,
    "indexesComposeWasm.file" to prefix + indexesComposeWasm,
    "libraries.folder.jvm" to prefix + libJVM,
    "libraries.folder.js" to prefix + libJS,
    "libraries.folder.wasm" to prefix + libWasm,
    "libraries.folder.compose-wasm" to prefix + libComposeWasm,
    "libraries.folder.compose-wasm-compiler-plugins" to prefix + libComposeWasmCompilerPlugins,
    "libraries.folder.compiler-plugins" to prefix + compilerPluginsForJVM,
    "spring.mvc.pathmatch.matching-strategy" to "ant_path_matcher",
    "server.compression.enabled" to "true",
    "server.compression.mime-types" to "application/json,text/javascript,application/wasm",
    "skiko.version" to libs.versions.skiko.get(),
)

val propertiesGenerator by tasks.registering(PropertiesGenerator::class) {
    dependsOn(kotlinComposeWasmStdlibFile)
    propertiesFile.fileValue(rootDir.resolve("src/main/resources/${propertyFile}"))
    hashableFile.fileProvider(
        provider {
            kotlinComposeWasmStdlibFile.singleFile
        }
    )
    generateProperties().forEach { (name, value) ->
        propertiesMap.put(name, value)
    }
}

val lambdaPropertiesGenerator by tasks.registering(PropertiesGenerator::class) {
    dependsOn(kotlinComposeWasmStdlibFile)
    propertiesFile.set(layout.buildDirectory.file("tmp/propertiesGenerator/${propertyFile}"))
    hashableFile.fileProvider(
        provider {
            kotlinComposeWasmStdlibFile.singleFile
        }
    )

    generateProperties(lambdaPrefix).forEach { (name, value) ->
        propertiesMap.put(name, value)
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.set(listOf("-Xjsr305=strict"))
    }
    dependsOn(":executors:jar")
    dependsOn(":indexation:run")
    dependsOn(propertiesGenerator)
}
println("Using Kotlin compiler ${libs.versions.kotlin.get()}")

tasks.withType<BootJar> {
    requiresUnpack("**/kotlin-*.jar")
    requiresUnpack("**/kotlinx-*.jar")
}

val buildLambda by tasks.creating(Zip::class) {
    val propertyFile = propertyFile

    from(tasks.compileKotlin)
    from(tasks.processResources) {
        exclude(propertyFile)
    }
    from(lambdaPropertiesGenerator)
    from(policy)
    from(indexes)
    from(indexesJs)
    from(indexesWasm)
    from(indexesComposeWasm)
    from(libJSFolder) { into(libJS) }
    from(libWasmFolder) { into(libWasm) }
    from(libComposeWasmFolder) { into(libComposeWasm) }
    from(libJVMFolder) { into(libJVM) }
    from(compilerPluginsForJVMFolder) { into(compilerPluginsForJVM) }
    from(libComposeWasmCompilerPluginsFolder) { into(libComposeWasmCompilerPlugins) }
    dependsOn(kotlinComposeWasmStdlibFile)
    into("lib") {
        from(configurations.compileClasspath) { exclude("tomcat-embed-*") }
    }
}

val prepareComposeWasmResources by tasks.registering(Sync::class) {
    from(composeWasmStaticResources)
    into(layout.buildDirectory.dir("compose-wasm-resources"))
}

tasks.named<Copy>("processResources") {
    dependsOn(propertiesGenerator)
}

tasks.withType<Test> {
    dependsOn(rootProject.the<NodeJsRootExtension>().nodeJsSetupTaskProvider)
    useJUnitPlatform()
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.AMAZON)
    })
    val executablePath = rootProject.the<NodeJsRootExtension>().requireConfigured().executable
    doFirst {
        this@withType.environment("kotlin.wasm.node.path", executablePath)
    }
}
