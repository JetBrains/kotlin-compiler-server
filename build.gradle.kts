import org.gradle.kotlin.dsl.support.serviceOf
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
    alias(libs.plugins.kotlin.plugin.spring)
    id("base-kotlin-jvm-conventions")
}

apply<NodeJsRootPlugin>()

allprojects {
    afterEvaluate {
        dependencies {
            dependencies {
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
                implementation(libs.kotlin.idea) {
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

    resourceDependency(libs.skiko.js.wasm.runtime)
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
    libraries.folder.jvm=${prefix + libJVM}
    libraries.folder.js=${prefix + libJS}
    libraries.folder.wasm=${prefix + libWasm}
    libraries.folder.compose-wasm=${prefix + libComposeWasm}
    libraries.folder.compose-wasm-compiler-plugins=${prefix + libComposeWasmCompilerPlugins}
    libraries.folder.compiler-plugins=${prefix + compilerPluginsForJVM}
    spring.mvc.pathmatch.matching-strategy=ant_path_matcher
    server.compression.enabled=true
    server.compression.mime-types=application/json,text/javascript,application/wasm
""".trimIndent()

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.set(listOf("-Xjsr305=strict"))
    }
    dependsOn(":executors:jar")
    dependsOn(":indexation:run")
    buildPropertyFile()
}
println("Using Kotlin compiler ${libs.versions.kotlin.get()}")

tasks.withType<BootJar> {
    requiresUnpack("**/kotlin-*.jar")
    requiresUnpack("**/kotlinx-*.jar")
}

val buildLambda by tasks.creating(Zip::class) {
    val propertyFile = propertyFile
    val propertyFileContent = generateProperties("/var/task/")

    from(tasks.compileKotlin)
    from(tasks.processResources) {
        eachFile {
            if (name == propertyFile) {
                file.writeText(propertyFileContent)
            }
        }
    }
    from(policy)
    from(indexes)
    from(indexesJs)
    from(indexesWasm)
    from(indexesComposeWasm)
    from(libJSFolder) { into(libJS) }
    from(libWasmFolder) { into(libWasm) }
    from(libComposeWasmFolder) { into(libComposeWasm) }
    from(libJVMFolder) { into(libJVM) }
    from(compilerPluginsForJVMFolder) {into(compilerPluginsForJVM)}
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
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.AMAZON)
    })
    val executablePath = rootProject.the<NodeJsRootExtension>().requireConfigured().executable
    doFirst {
        this@withType.environment("kotlin.wasm.node.path", executablePath)
    }
}
