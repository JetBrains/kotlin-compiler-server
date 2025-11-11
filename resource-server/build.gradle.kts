import org.apache.tools.ant.filters.ConcatFilter
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.io.StringReader
import java.util.*

plugins {
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.plugin.spring)
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
    propertiesMap.put("server.port", "8081")
    propertiesMap.put("skiko.version", libs.versions.skiko.get())

    val applicationPropertiesPath = projectDir.resolve("src/main/resources/application.properties")

    propertiesFile.fileValue(applicationPropertiesPath)

    val composeWasmStdlibFile: FileCollection = kotlinComposeWasmRuntime

    hashableDir.from(composeWasmStdlibFile)
}

tasks.withType<KotlinCompile> {
    dependsOn(kotlinComposeWasmRuntime)
    dependsOn(propertiesGenerator)
}

val skikoVersion = libs.versions.skiko

val prepareComposeWasmResources by tasks.registering(Sync::class) {
    dependsOn(kotlinComposeWasmRuntime)
    dependsOn(propertiesGenerator)

    into(layout.buildDirectory.dir("tmp/prepareResources"))

    val propertiesFile = propertiesGenerator.flatMap { it.propertiesFile }

    from(kotlinComposeWasmRuntime) {
        include("**/*.uninstantiated.mjs", "skiko.mjs", "**/*.wasm", "@js-joda/**")

        rename { original ->
            val properties = FileInputStream(propertiesFile.get().asFile).use {
                Properties().apply {
                    load(it)
                }
            }
            val regex = Regex("^(.+?)(\\.uninstantiated)*\\.(mjs|wasm)\$")
            regex.find(original)?.groupValues?.let { groups ->
                val name = groups[1]
                val uninst: String = groups[2]
                val extension = groups[3]
                "$name-${properties["dependencies.compose-wasm"]}$uninst.$extension"
            } ?: original

        }

        includeEmptyDirs = false

        filesMatching("@js-joda/**") {
            val properties = FileInputStream(propertiesFile.get().asFile).use {
                Properties().apply {
                    load(it)
                }
            }
            path = path.replace("@js-joda", "@js-joda-${properties["dependencies.compose-wasm"]}")
        }

        filesMatching(listOf("_kotlin_.uninstantiated.mjs")) {
            val header = """
                class BufferedOutput {
                    constructor() {
                        this.buffer = ""
                    }
                }
                globalThis.bufferedOutput = new BufferedOutput()
            """.trimIndent()

            filter(mapOf("prependReader" to StringReader(header)), ConcatFilter::class.java)

            filter { line: String ->
                line.replace(
                    "const importObject = {",
                    "js_code['kotlin.io.printImpl'] = (message) => globalThis.bufferedOutput.buffer += message\n" +
                            "js_code['kotlin.io.printlnImpl'] = (message) => {globalThis.bufferedOutput.buffer += message;bufferedOutput.buffer += \"\\n\"}\n" +
                            "const importObject = {"
                )
            }
        }

        filesMatching(listOf("**/*.uninstantiated.mjs", "skiko.mjs")) {
            filter { line: String ->
                val properties = FileInputStream(propertiesFile.get().asFile).use {
                    Properties().apply {
                        load(it)
                    }
                }

                val composeWasmHash = properties["dependencies.compose-wasm"]
                line
                    .replace(".wasm\'", "-$composeWasmHash.wasm\'")
                    .replace(".uninstantiated.mjs\')", "-$composeWasmHash.uninstantiated.mjs\')")
                    .replace("skiko.mjs\')", "skiko-$composeWasmHash.mjs\')")
                    .replace("skiko.wasm\"", "skiko-$composeWasmHash.wasm\"")
                    .replace(
                        "import('@js-joda/core')",
                        "import('./@js-joda-${composeWasmHash}/core/dist/js-joda.esm.js')"
                    )
            }
        }
    }
}

tasks.named<Copy>("processResources") {
    dependsOn(prepareComposeWasmResources)
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