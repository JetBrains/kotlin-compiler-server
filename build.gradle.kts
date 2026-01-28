import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

val policy: String by System.getProperties()

group = "com.compiler.server"
version = "${libs.versions.kotlin.get()}-SNAPSHOT"

val propertyFile = "application.properties"

plugins {
    id("base-spring-boot-conventions")
}

apply<NodeJsRootPlugin>()

val kotlinComposeWasmCache: Configuration by configurations.creating {
    isTransitive = false
    isCanBeResolved = false
    isCanBeConsumed = false
}

val kotlinComposeWasmRuntime: Configuration by configurations.creating {
    isTransitive = false
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(
            Category.CATEGORY_ATTRIBUTE,
            objects.categoryComposeCache
        )
    }
    extendsFrom(kotlinComposeWasmCache)
}

val kotlinComposeWasmRuntimeHash: Configuration by configurations.creating {
    isTransitive = false
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(
            Category.CATEGORY_ATTRIBUTE,
            objects.categoryComposeCacheHash
        )
    }
    extendsFrom(kotlinComposeWasmCache)
}

dependencies {
    annotationProcessor(libs.spring.context.indexer)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.aws.springboot.container)
    implementation(libs.springdoc.webmvc)
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.compiler.arguments.description)
    implementation(libs.junit)
    implementation(libs.logback.logstash.encoder)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.script.runtime)
    implementation(libs.kotlin.build.tools.api)
    implementation(libs.kotlin.build.tools.impl)
    implementation(libs.kotlin.compiler.embeddable)
    implementation(libs.kotlin.tooling.core)
    implementation(libs.jackson.module.kotlin)
    implementation(project(":executors", configuration = "default"))
    implementation(project(":common", configuration = "default"))
    implementation(project(":dependencies"))

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)

    kotlinComposeWasmCache(project(":cache-maker"))
}

fun Project.generateProperties(
    prefix: String = "",
): Map<String, String> = mapOf(
    "server.error.include-message" to "always",
    "server.error.include-binding-errors" to "always",
    "kotlin.version" to kotlinVersion,
    "policy.file" to prefix + policy,
    "libraries.folder.jvm" to prefix + libJVM,
    "libraries.folder.js" to prefix + libJS,
    "libraries.folder.wasm" to prefix + libWasm,
    "libraries.folder.compose-wasm" to prefix + libComposeWasm,
    "libraries.folder.compose-wasm-compiler-plugins" to prefix + libComposeWasmCompilerPlugins,
    "libraries.folder.compiler-plugins" to prefix + compilerPluginsForJVM,
    "spring.mvc.pathmatch.matching-strategy" to "ant_path_matcher",
    "server.compression.enabled" to "true",
    "server.compression.mime-types" to "application/json,text/javascript,application/wasm",
    "springdoc.swagger-ui.path" to "/api-docs/swagger-ui.html",
)

fun MapProperty<String, String>.fillProperties(
    prefix: String = "",
) {
    generateProperties(prefix).forEach { (name, value) ->
        put(name, value)
    }

    val kotlinComposeWasmRuntimeHash: FileCollection = kotlinComposeWasmRuntimeHash

    val hashValue: Provider<String> = kotlinComposeWasmRuntimeHash.elements.map {
        it.single().asFile.readText()
    }

    put(
        dependenciesComposeWasm,
        hashValue
    )

    put(
        dependenciesStaticUrl,
        providers.gradleProperty(DEPENDENCIES_STATIC_URL).orElse(localhostStaticUrl)
    )
}

val propertiesGenerator by tasks.registering(PropertiesGenerator::class) {
    dependsOn(kotlinComposeWasmRuntime)
    dependsOn(kotlinComposeWasmRuntimeHash)

    propertiesFile.fileValue(rootDir.resolve("src/main/resources/${propertyFile}"))

    propertiesMap.fillProperties()
}

val lambdaPropertiesGenerator by tasks.registering(PropertiesGenerator::class) {
    dependsOn(kotlinComposeWasmRuntime)
    propertiesFile.set(layout.buildDirectory.file("tmp/propertiesGenerator/${propertyFile}"))

    propertiesMap.fillProperties(lambdaPrefix)
}

tasks.withType<KotlinCompile> {
    dependsOn(":executors:jar")
    dependsOn(propertiesGenerator)
}
println("Using Kotlin compiler ${libs.versions.kotlin.get()}")

tasks.withType<BootJar> {
    requiresUnpack("**/kotlin-*.jar")
    requiresUnpack("**/kotlinx-*.jar")
}

val prepareComposeWasmResources by tasks.registering(Sync::class) {
    from(kotlinComposeWasmRuntime)
    into(layout.buildDirectory.dir("compose-wasm-resources"))
}

val buildLambda by tasks.creating(Zip::class) {
    val propertyFile = propertyFile

    from(tasks.compileKotlin)
    from(tasks.processResources) {
        exclude(propertyFile)
    }
    from(lambdaPropertiesGenerator)
    from(policy)
    from(libJSFolder) { into(libJS) }
    from(libWasmFolder) { into(libWasm) }
    from(libComposeWasmFolder) { into(libComposeWasm) }
    from(libJVMFolder) { into(libJVM) }
    from(compilerPluginsForJVMFolder) { into(compilerPluginsForJVM) }
    from(libComposeWasmCompilerPluginsFolder) { into(libComposeWasmCompilerPlugins) }
    dependsOn(kotlinComposeWasmRuntime)
    into("lib") {
        from(configurations.productionRuntimeClasspath) { exclude("tomcat-embed-*") }
    }

    dependsOn(prepareComposeWasmResources)
}

tasks.named<Copy>("processResources") {
    dependsOn(propertiesGenerator)
}

tasks.withType<Test> {
    with(rootProject.kotlinNodeJsEnvSpec) {
        dependsOn(rootProject.nodeJsSetupTaskProvider)
    }
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.AMAZON)
    })
    val executablePath = rootProject.kotlinNodeJsEnvSpec.executable.get()
    doFirst {
        this@withType.environment("kotlin.wasm.node.path", executablePath)
    }

    // We disable this on TeamCity, because we don't want to fail this test,
    // when compiler server's test run as a K2 user project.
    // But for our pull requests we still need to run this test, so we add it to our GitHub action.
    if (System.getenv("TEAMCITY_VERSION") != null) {
        filter {
            excludeTestsMatching("com.compiler.server.CompilerArguments*")
        }
    }
}
