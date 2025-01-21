import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files
import kotlin.io.path.createFile

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

val kotlinComposeWasmStdlibTypeInfo: Configuration by configurations.creating {
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
            CacheAttribute.TYPEINFO
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
    kotlinComposeWasmStdlibTypeInfo(project(":cache-maker"))
}

val composeWasmPropertiesUpdater by tasks.registering(ComposeWasmPropertiesUpdater::class) {
    dependsOn(kotlinComposeWasmStdlibTypeInfo)
    propertiesMap.put("spring.mvc.pathmatch.matching-strategy", "ant_path_matcher")
    propertiesMap.put("server.port", "8081")
    propertiesMap.put("skiko.version", libs.versions.skiko.get())

    val applicationPropertiesPath = projectDir.resolve("src/main/resources/application.properties")

    if (!applicationPropertiesPath.exists()) {
        applicationPropertiesPath.createNewFile()
    }

//    propertiesPath.set(applicationPropertiesPath.normalize().absolutePath)
//
//    val composeWasmStdlibTypeInfo: FileCollection = kotlinComposeWasmStdlibTypeInfo
//
//    typeInfoFile.fileProvider(
//        provider {
//            composeWasmStdlibTypeInfo.singleFile
//        }
//    )
}

tasks.withType<KotlinCompile> {
    dependsOn(composeWasmPropertiesUpdater)
}

tasks.named<Copy>("processResources") {
    dependsOn(composeWasmPropertiesUpdater)
    val archiveOperation = project.serviceOf<ArchiveOperations>()
    from(resourceDependency.map {
        archiveOperation.zipTree(it)
    }) {
        into("com/compiler/server")
    }
    from(kotlinComposeWasmStdlib) {
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