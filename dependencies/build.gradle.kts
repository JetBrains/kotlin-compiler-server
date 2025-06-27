import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute

plugins {
    id("base-kotlin-jvm-conventions")
}

val kotlinDependency: Configuration by configurations.creating {
    isTransitive = false
}

val kotlinCompilerPluginDependency: Configuration by configurations.creating {
    isTransitive = false
    isCanBeResolved = true
    isCanBeConsumed = false
}

val kotlinJsDependency: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false

    attributes {
        attribute(
            KotlinPlatformType.attribute,
            KotlinPlatformType.js
        )
        attribute(
            KotlinJsCompilerAttribute.jsCompilerAttribute,
            KotlinJsCompilerAttribute.ir
        )
    }
}

val kotlinWasmDependency: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false

    attributes {
        attribute(
            KotlinPlatformType.attribute,
            KotlinPlatformType.wasm
        )
        attribute(
            KotlinWasmTargetAttribute.wasmTargetAttribute,
            KotlinWasmTargetAttribute.js
        )
    }
}

val kotlinComposeWasmDependency: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false

    attributes {
        attribute(
            KotlinPlatformType.attribute,
            KotlinPlatformType.wasm
        )
        attribute(
            KotlinWasmTargetAttribute.wasmTargetAttribute,
            KotlinWasmTargetAttribute.js
        )
    }
}

val composeWasmCompilerPlugins: Configuration by configurations.creating {
    isTransitive = false
}

val composeRuntimeVersion = "1.6.0"

val copyDependencies by tasks.creating(Copy::class) {
    from(kotlinDependency)
    into(libJVMFolder)
}

val copyCompilerPluginDependencies by tasks.creating(Copy::class) {
    from(kotlinCompilerPluginDependency)
    into(compilerPluginsForJVMFolder)
}

val copyJSDependencies by tasks.creating(Copy::class) {
    from(kotlinJsDependency)
    into(libJSFolder)
}

val copyWasmDependencies by tasks.creating(Copy::class) {
    from(kotlinWasmDependency)
    into(libWasmFolder)
}

val copyComposeWasmDependencies by tasks.creating(Copy::class) {
    from(kotlinComposeWasmDependency)
    into(libComposeWasmFolder)
}

val copyComposeWasmCompilerPlugins by tasks.creating(Copy::class) {
    from(composeWasmCompilerPlugins)
    into(libComposeWasmCompilerPluginsFolder)
}

dependencies {
    kotlinDependency(libs.junit)
    kotlinDependency(libs.junit.jupiter)
    kotlinDependency(libs.junit.jupiter.api)
    kotlinDependency(libs.hamcrest)
    kotlinDependency(libs.bundles.jackson)
    // Kotlin libraries
    kotlinDependency(libs.bundles.kotlin.stdlib)
    kotlinDependency(libs.kotlin.test)
    kotlinDependency(libs.kotlin.test.junit)
    kotlinDependency(libs.kotlinx.coroutines.core.jvm)
    kotlinDependency(libs.kotlinx.coroutines.test)
    kotlinDependency(libs.kotlinx.datetime)
    kotlinDependency(libs.kotlinx.io.bytestring)
    kotlinDependency(libs.kotlinx.io.core)
    kotlinDependency(libs.kotlinx.serialization.json.jvm)
    kotlinDependency(libs.kotlinx.serialization.core.jvm)
    kotlinCompilerPluginDependency(libs.kotlin.serialization.plugin)
    kotlinJsDependency(libs.kotlin.stdlib.js)
    kotlinJsDependency(libs.kotlin.dom.api.compat)
    kotlinWasmDependency(libs.kotlin.stdlib.wasm.js)

    // compose
    kotlinComposeWasmDependency(libs.kotlin.stdlib.wasm.js)
    kotlinComposeWasmDependency(libs.bundles.compose)

    composeWasmCompilerPlugins(libs.kotlin.compose.compiler.plugin)
}