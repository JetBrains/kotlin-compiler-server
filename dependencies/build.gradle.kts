import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute

val kotlinDependency: Configuration by configurations.creating {
    isTransitive = false
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


val jacksonVersionKotlinDependencyJar = "2.14.0" // don't forget to update version in `executor.policy` file.

val copyDependencies by tasks.creating(Copy::class) {
    from(kotlinDependency)
    into(libJVMFolder)
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

plugins {
    kotlin("jvm")
}

dependencies {
    kotlinDependency("junit:junit:4.13.2")
    kotlinDependency("org.hamcrest:hamcrest:2.2")
    kotlinDependency("com.fasterxml.jackson.core:jackson-databind:$jacksonVersionKotlinDependencyJar")
    kotlinDependency("com.fasterxml.jackson.core:jackson-core:$jacksonVersionKotlinDependencyJar")
    kotlinDependency("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersionKotlinDependencyJar")
    // Kotlin libraries
    kotlinDependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    kotlinDependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    kotlinDependency("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    kotlinDependency("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    kotlinDependency("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3")
    kotlinDependency("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    kotlinDependency("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0-RC.2")
    kotlinJsDependency("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")
    kotlinJsDependency("org.jetbrains.kotlin:kotlin-dom-api-compat:$kotlinVersion")
    kotlinWasmDependency("org.jetbrains.kotlin:kotlin-stdlib-wasm-js:$kotlinVersion")

    // compose
    kotlinComposeWasmDependency("org.jetbrains.kotlin:kotlin-stdlib-wasm-js:$kotlinVersion")
    kotlinComposeWasmDependency("org.jetbrains.compose.runtime:runtime:1.6.0")
    kotlinComposeWasmDependency("org.jetbrains.compose.ui:ui:1.6.0")
    kotlinComposeWasmDependency("org.jetbrains.compose.animation:animation:1.6.0")
    kotlinComposeWasmDependency("org.jetbrains.compose.animation:animation-graphics:1.6.0")
    kotlinComposeWasmDependency("org.jetbrains.compose.foundation:foundation:1.6.0")
    kotlinComposeWasmDependency("org.jetbrains.compose.material:material:1.6.0")
    kotlinComposeWasmDependency("org.jetbrains.compose.components:components-resources:1.6.0")

    composeWasmCompilerPlugins("org.jetbrains.kotlin:kotlin-compose-compiler-plugin:$kotlinVersion")
}