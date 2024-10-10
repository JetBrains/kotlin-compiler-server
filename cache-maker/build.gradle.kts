import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.plugin.compose)
}

val composeWasmCaches: Configuration by configurations.creating {
    isVisible = false
    isCanBeResolved = false
    isCanBeConsumed = true

    attributes {
        attribute(
            KotlinPlatformType.attribute,
            KotlinPlatformType.wasm
        )
        attribute(
            KotlinWasmTargetAttribute.wasmTargetAttribute,
            KotlinWasmTargetAttribute.js
        )
        attribute(
            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
            objects.named(LibraryElements::class.java, "compose-wasm-cache")
        )
    }
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        val executables = binaries.executable()

        val linkTask = executables
            .single { it.mode == KotlinJsBinaryMode.DEVELOPMENT }
            .linkTask

        val rootCacheDir = linkTask
            .map { it.rootCacheDirectory }

        artifacts.add(composeWasmCaches.name, rootCacheDir) {
            builtBy(linkTask)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.bundles.compose)
        }
    }
}