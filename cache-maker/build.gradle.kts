@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenExec
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.WasmBinary

plugins {
    kotlin("multiplatform")
}

kotlin {
    wasmJs {
        outputModuleName.set("stdlib")
        binaries.executable().forEach { binary: JsIrBinary ->
            binary.linkTask.configure {
                compilerOptions.freeCompilerArgs.add("-Xir-dce=false")
                compilerOptions.freeCompilerArgs.add("-Xwasm-multimodule-mode=master")
            }

            (binary as WasmBinary).optimizeTask.configure {
                inputFileProperty.fileProvider(
                    binary.mainFile.map {
                        val file = it.asFile
                        file.resolveSibling("${file.nameWithoutExtension}_master.wasm")
                    }
                )
            }
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

val compileProductionExecutableKotlinWasmJsOptimize: TaskProvider<BinaryenExec> by tasks.existing(BinaryenExec::class) {
}

val composeWasmStdlib: Provider<Directory> = compileProductionExecutableKotlinWasmJsOptimize
    .flatMap { it.outputDirectory.locationOnly }
val composeWasmStdlibFile: Provider<RegularFile> = composeWasmStdlib
    .map { it.file("stdlib_master.wasm") }

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootPlugin> {
    rootProject.the<BinaryenRootEnvSpec>().version = "122"
}

val kotlinComposeWasmStdlibFile: Configuration by configurations.creating {
    isTransitive = false
    isCanBeResolved = false
    isCanBeConsumed = true
    attributes {
        attribute(
            Category.CATEGORY_ATTRIBUTE,
            objects.categoryComposeCache
        )
    }
}

kotlinComposeWasmStdlibFile.outgoing.variants.create("stdlib") {
    attributes {
        attribute(
            CacheAttribute.cacheAttribute,
            CacheAttribute.FULL
        )
    }

    artifact(composeWasmStdlib) {
        builtBy(compileProductionExecutableKotlinWasmJsOptimize)
    }
}

kotlinComposeWasmStdlibFile.outgoing.variants.create("wasm-file") {
    attributes {
        attribute(
            CacheAttribute.cacheAttribute,
            CacheAttribute.WASM
        )
    }

    artifact(composeWasmStdlibFile) {
        builtBy(compileProductionExecutableKotlinWasmJsOptimize)
    }
}

// we don't need to build cache-maker
tasks.named("build") {
    dependsOn.clear()
}
