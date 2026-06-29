@file:OptIn(ExperimentalWasmDsl::class)

import org.apache.tools.ant.filters.ConcatFilter
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import java.io.StringReader

plugins {
    id("base-kotlin-multiplatform-conventions")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    wasmJs {
        nodejs()
        val productionMainExecutable = binaries.executable().filter { it.mode == KotlinJsBinaryMode.PRODUCTION }.single()

        val prepareRuntime by tasks.registering(Copy::class) {
            val allRuntimes: Provider<File> = productionMainExecutable.linkSyncTask.flatMap { it.destinationDirectory }

            with(project.the<BinaryenEnvSpec>()) {
                dependsOn(project.binaryenSetupTaskProvider)
            }

            val rootProjectName = rootProject.name
            val projectName = project.name

            from(allRuntimes) {
                include {
                    it.name.endsWith(".mjs") || it.name.endsWith(".wasm")
                }
                exclude {
                    it.name.contains("$rootProjectName-$projectName")
                }
            }

            val npmInstallTaskProvider = rootProject.the<WasmNodeJsRootExtension>().npmInstallTaskProvider

            dependsOn(npmInstallTaskProvider)

            val nodeModulesDir: Provider<Directory> = npmInstallTaskProvider.flatMap {
                it.nodeModules
            }

            from(nodeModulesDir.map { it.dir("@js-joda") }) {
                into("@js-joda")
            }

            into(layout.buildDirectory.dir("all-libs"))
        }

        val calculatedHash = layout.buildDirectory.file("tmp/calculateHash/hash")

        val calculateHash by tasks.registering(DefaultTask::class) {
            val destDir = prepareRuntime.map { it.destinationDir }
            inputs.dir(destDir)
            val outputFile = calculatedHash
            outputs.file(outputFile)

            doLast {
                outputFile.get().asFile.also {
                    it.parentFile.mkdirs()
                    it.writeText(hashFileContent(destDir.get()))
                }
            }
        }

        val prepareComposeWasmResources by tasks.registering(Sync::class) {
            into(layout.buildDirectory.dir("tmp/prepareResources"))

            dependsOn(calculateHash)

            val calculatedHash = calculatedHash

            inputs.file(calculatedHash)

            from(prepareRuntime) {
                rename { original ->

                    val hash = calculatedHash.get().asFile.readText()
                    val regex = Regex("^(.+?)\\.(mjs|wasm)$")
                    regex.find(original)?.groupValues?.let { groups ->
                        val name = groups[1]
                        val extension = groups[2]
                        "$name-$hash.$extension"
                    } ?: original

                }

                includeEmptyDirs = false

                filesMatching("@js-joda/**") {
                    val hash = calculatedHash.get().asFile.readText()
                    path = path.replace("@js-joda", "@js-joda-$hash")
                }

                filesMatching(listOf("kotlin-kotlin-stdlib.mjs")) {
                    val header = """
                class BufferedOutput {
                    constructor() {
                        this.buffer = ""
                    }
                }
                globalThis.bufferedOutput = new BufferedOutput()
            """.trimIndent()

                    filter(mapOf("prependReader" to StringReader(header)), ConcatFilter::class.java)
                }

                filesMatching(listOf("kotlin-kotlin-stdlib.import-object.mjs")) {
                    filter { line: String ->
                        line.replace(
                            "export const importObject = {",
                            "js_code['kotlin.io.printImpl'] = (message) => globalThis.bufferedOutput.buffer += message\n" +
                                    "js_code['kotlin.io.printlnImpl'] = (message) => {globalThis.bufferedOutput.buffer += message;bufferedOutput.buffer += \"\\n\"}\n" +
                                    "export const importObject = {"
                        )
                    }
                }

                filesMatching(listOf("**/*.mjs", "skiko.mjs")) {
                    filter { line: String ->
                        val hash = calculatedHash.get().asFile.readText()

                        line
                            .replace(".mjs\':", "<TEMP_IMPORT>")
                            .replace(".wasm\'", "-$hash.wasm\'")
                            .replace(".mjs\'", "-$hash.mjs\'")
                            .replace(".mjs\"", "-$hash.mjs\"")
                            .replace("skiko.wasm\"", "skiko-$hash.wasm\"")
                            .replace(
                                "'@js-joda/core'",
                                "'./@js-joda-${hash}/core/dist/js-joda.esm.js'"
                            )
                            .replace("<TEMP_IMPORT>", ".mjs\':")
                    }
                }
            }
        }

        val bundledRuntimesDir = layout.buildDirectory.dir("tmp/bundledRuntimes")

        val bundledRuntimes by tasks.registering(DefaultTask::class) {
            val inputDir = prepareComposeWasmResources.map { it.destinationDir }
            inputs.dir(inputDir)
            val outputDir = bundledRuntimesDir
            outputs.dir(outputDir)

            val staticUrl = providers.gradleProperty(DEPENDENCIES_STATIC_URL)
            inputs.property("staticUrl", staticUrl).optional(true)

            val fs = project.serviceOf<FileSystemOperations>()

            doLast {
                outputDir.get().asFile.mkdirs()

                inputDir.get().listFiles()
                    .filter { it.extension == "wasm" }
                    .forEach { wasmFile ->

                        val nameWithoutExtension = wasmFile.nameWithoutExtension
                        val relevantName = nameWithoutExtension.substringBeforeLast("-")
                        val hash = nameWithoutExtension.substringAfterLast("-")

                        if (relevantName == "skiko") return@forEach

                        val importObject = wasmFile.parentFile.resolve("$relevantName.import-object-${hash}.mjs")
                        val jsBuiltIns = wasmFile.parentFile.resolve("$relevantName.js-builtins-$hash.mjs").takeIf { it.exists() }
                        val mainMjs = wasmFile.parentFile.resolve("$nameWithoutExtension.mjs")

                        val resultMjs = mergeWasmOutputIntoOneJs(
                            jsBuiltIns?.readText(),
                            importObject.readText(),
                            mainMjs.readText(),
                            wasmFile.readBytes(),
                            relevantName,
                            hash,
                            staticUrl.orElse(localhostStaticUrl).get()
                        )

                        outputDir.get().asFile.resolve("$nameWithoutExtension.mjs").writeText(resultMjs)
                    }

                fs.copy {
                    from(inputDir) {
                        include {
                            it.name.substringBeforeLast("-") == "skiko"
                        }
                        include("@js-joda-*/**")
                    }
                    into(outputDir)
                }
            }
        }

        val kotlinComposeWasmRuntime: Configuration by configurations.creating {
            isTransitive = false
            isCanBeResolved = false
            isCanBeConsumed = true
            attributes {
                attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    objects.categoryComposeCache
                )
            }

            outgoing.variants.create("all", Action {
                artifact(bundledRuntimesDir) {
                    builtBy(bundledRuntimes)
                }
            })
        }

        val kotlinComposeWasmRuntimeHash: Configuration by configurations.creating {
            isTransitive = false
            isCanBeResolved = false
            isCanBeConsumed = true

            attributes {
                attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    objects.categoryComposeCacheHash
                )
            }

            outgoing.variants.create("hash", Action {
                artifact(calculatedHash) {
                    builtBy(calculateHash)
                }
            })
        }
    }

    sourceSets {
        wasmJsMain {
            dependencies {
                implementation(libs.bundles.compose)
                implementation(libs.kotlinx.coroutines.core.compose.wasm)
            }
        }
    }
}

// we don't need to build cache-maker
tasks.named("build") {
    dependsOn.clear()
}