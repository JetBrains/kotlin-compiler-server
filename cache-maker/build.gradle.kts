plugins {
    kotlin("multiplatform")
}

kotlin {
    wasmJs {
        outputModuleName.set("stdlib")
        binaries.executable().forEach {
            it.linkTask.configure {
                compilerOptions.freeCompilerArgs.add("-Xir-dce=false")
                compilerOptions.freeCompilerArgs.add("-Xwasm-multimodule-mode=master")
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

val composeWasmStdlib: Provider<Directory> = layout.buildDirectory
    .dir("compose-wasm-stdlib-output")
val composeWasmStdlibTypeInfo: Provider<RegularFile> = composeWasmStdlib
    .map { it.file("stdlib_master.wasm") }

val buildComposeWasmStdlibModule by tasks.registering(Exec::class) {

    inputs.files(configurations.named("wasmJsRuntimeClasspath"))

    workingDir = rootDir
    executable = "${project.name}/docker-build-incremental-cache.sh"

    val outputDir = composeWasmStdlib

    inputs.file(layout.projectDirectory.file("Dockerfile"))
    inputs.file(layout.projectDirectory.file("docker-build-incremental-cache.sh"))
    outputs.dir(outputDir)

    args(lambdaPrefix, outputDir.get().asFile.normalize().absolutePath)
}

val prepareTypeInfoIntoComposeWasmCache by tasks.registering(Sync::class) {
    dependsOn(buildComposeWasmStdlibModule)
    from(composeWasmStdlibTypeInfo)
    into(cachesComposeWasmFolder)
}

val kotlinComposeWasmStdlibTypeInfo: Configuration by configurations.creating {
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

kotlinComposeWasmStdlibTypeInfo.outgoing.variants.create("stdlib") {
    attributes {
        attribute(
            CacheAttribute.cacheAttribute,
            CacheAttribute.FULL
        )
    }

    artifact(composeWasmStdlib) {
        builtBy(buildComposeWasmStdlibModule)
    }
}

kotlinComposeWasmStdlibTypeInfo.outgoing.variants.create("typeinfo") {
    attributes {
        attribute(
            CacheAttribute.cacheAttribute,
            CacheAttribute.TYPEINFO
        )
    }

    artifact(composeWasmStdlibTypeInfo) {
        builtBy(prepareTypeInfoIntoComposeWasmCache)
    }
}

// we don't need to build cache-maker
tasks.named("build") {
    dependsOn.clear()
}
