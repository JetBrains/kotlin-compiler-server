plugins {
    kotlin("multiplatform")
}

kotlin {
    wasmJs {
        outputModuleName.set("stdlib")
        binaries.executable().forEach {
            it.linkTask.configure {
                compilerOptions.freeCompilerArgs.add("-Xir-dce=false")
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

val composeWasmStdlibTypeInfo: Provider<RegularFile> = layout.buildDirectory
    .file("compose-wasm-stdlib-output/stdlib.typeinfo.bin")

val buildComposeWasmStdlibModule by tasks.registering(Exec::class) {
    workingDir = rootDir
    executable = "${project.name}/docker-build-incremental-cache.sh"

    val outputDir = composeWasmStdlibTypeInfo.map { it.asFile.parentFile }

    inputs.file(layout.projectDirectory.file("Dockerfile"))
    inputs.file(layout.projectDirectory.file("docker-build-incremental-cache.sh"))
    outputs.dir(outputDir)

    argumentProviders.add {
        listOf(
            lambdaPrefix, // baseDir
            outputDir.get().normalize().absolutePath, // targetDir
        )
    }
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
            CacheAttribute.STDLIB
        )
    }

    artifact(cachesComposeWasmFolder) {
        type = "directory"
        builtBy(prepareTypeInfoIntoComposeWasmCache)
    }
}
