import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.util.*

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

val resourceDependency: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
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

val kotlinComposeWasmStdlib: Configuration by configurations.creating {
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
            CacheAttribute.FULL
        )
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    resourceDependency(libs.skiko.js.wasm.runtime)
    kotlinComposeWasmStdlib(project(":cache-maker"))
    kotlinComposeWasmStdlibFile(project(":cache-maker"))
}

val propertiesGenerator by tasks.registering(PropertiesGenerator::class) {
    dependsOn(kotlinComposeWasmStdlibFile)
    propertiesMap.put("spring.mvc.pathmatch.matching-strategy", "ant_path_matcher")
    propertiesMap.put("server.port", "8081")
    propertiesMap.put("skiko.version", libs.versions.skiko.get())

    val applicationPropertiesPath = projectDir.resolve("src/main/resources/application.properties")

    propertiesFile.fileValue(applicationPropertiesPath)

    val composeWasmStdlibFile: FileCollection = kotlinComposeWasmStdlibFile

    hashableFile.fileProvider(
        provider {
            composeWasmStdlibFile.singleFile
        }
    )
}

tasks.withType<KotlinCompile> {
    dependsOn(kotlinComposeWasmStdlibFile)
    dependsOn(propertiesGenerator)
}

val skikoVersion = libs.versions.skiko

val prepareComposeWasmResources by tasks.registering(Sync::class) {
    dependsOn(kotlinComposeWasmStdlibFile)
    dependsOn(propertiesGenerator)
    val archiveOperation = project.serviceOf<ArchiveOperations>()

    into(layout.buildDirectory.dir("tmp/prepareResources"))

    from(resourceDependency.map {
        archiveOperation.zipTree(it)
    }) {
        rename("skiko\\.(.*)", "skiko-${skikoVersion.get()}.\$1")
        include("skiko.mjs", "skiko.wasm")
    }

    val propertiesFile = propertiesGenerator.flatMap { it.propertiesFile }

    from(kotlinComposeWasmStdlib) {
        include("stdlib_master.uninstantiated.mjs", "stdlib_master.wasm")

        rename { original ->
            val properties = FileInputStream(propertiesFile.get().asFile).use {
                Properties().apply {
                    load(it)
                }
            }
            val regex = Regex("stdlib_master(\\.uninstantiated)*\\.(.*)")
            regex.find(original)?.groupValues?.get(2)?.let { extension ->
                "stdlib-${properties["dependencies.compose.wasm"]}.$extension"
            } ?: original

        }
    }
}

tasks.named<Copy>("processResources") {
    dependsOn(prepareComposeWasmResources)
    from(prepareComposeWasmResources) {
        into("com/compiler/server")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.AMAZON)
    })
}

val composeWasmStaticResources: Configuration by configurations.creating {
    isTransitive = false
    isCanBeResolved = false
    isCanBeConsumed = true
    attributes {
        attribute(
            Category.CATEGORY_ATTRIBUTE,
            objects.categoryComposeWasmResources
        )
    }

    outgoing.artifact(prepareComposeWasmResources) {
        builtBy(prepareComposeWasmResources)
    }
}