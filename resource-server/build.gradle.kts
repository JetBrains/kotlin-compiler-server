import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("base-spring-boot-conventions")
    id("base-kotlin-jvm-conventions")
}

val resourceDependency: Configuration by configurations.creating {
    isCanBeResolved = true
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
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    resourceDependency(libs.skiko.js.wasm.runtime)
    kotlinComposeWasmRuntime(project(":cache-maker"))
}

val propertiesGenerator by tasks.registering(PropertiesGenerator::class) {
    dependsOn(kotlinComposeWasmRuntime)
    propertiesMap.put("spring.mvc.pathmatch.matching-strategy", "ant_path_matcher")
    propertiesMap.put("server.port", staticPort)

    val applicationPropertiesPath = projectDir.resolve("src/main/resources/application.properties")

    propertiesFile.fileValue(applicationPropertiesPath)
}

tasks.withType<KotlinCompile> {
    dependsOn(kotlinComposeWasmRuntime)
    dependsOn(propertiesGenerator)
}

val prepareComposeWasmResources by tasks.registering(Sync::class) {
    dependsOn(kotlinComposeWasmRuntime)

    into(layout.buildDirectory.dir("tmp/prepareResources"))

    from(kotlinComposeWasmRuntime)
}

tasks.named<Copy>("processResources") {
    dependsOn(prepareComposeWasmResources)
    dependsOn(propertiesGenerator)
    from(prepareComposeWasmResources) {
        into("static")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.AMAZON)
    })
}