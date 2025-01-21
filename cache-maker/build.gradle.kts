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

val composeWasmStdlib: Provider<Directory> = layout.buildDirectory
    .dir("compose-wasm-stdlib-output")
val composeWasmStdlibTypeInfo: Provider<RegularFile> = composeWasmStdlib
    .map { it.file("compose-wasm-stdlib-output/stdlib.typeinfo.bin") }

val buildComposeWasmStdlibModule by tasks.registering(Exec::class) {

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
        builtBy(prepareTypeInfoIntoComposeWasmCache)
    }
}

kotlinComposeWasmStdlibTypeInfo.outgoing.variants.create("typeinfo") {
    attributes {
        attribute(
            CacheAttribute.cacheAttribute,
            CacheAttribute.TYPEINFO
        )
    }

    artifact(cachesComposeWasmFolder.file("stdlib.typeinfo.bin")) {
        builtBy(prepareTypeInfoIntoComposeWasmCache)
    }
}
