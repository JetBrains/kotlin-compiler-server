plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":common", configuration = "default"))
}

application {
    mainClass.set("com.compiler.server.cache.MainKt")
}

val runTask = tasks.named<JavaExec>("run") {
    dependsOn(":dependencies:copyDependencies")
    dependsOn(":dependencies:copyWasmDependencies")
    dependsOn(":dependencies:copyComposeWasmCompilerPlugins")
    dependsOn(":dependencies:copyComposeWasmDependencies")

    val kotlinVersion = libs.versions.kotlin.get()
    inputs.property("kotlinVersion", kotlinVersion)

    inputs.dir(libWasmFolder)
    inputs.dir(libComposeWasmFolder)
    inputs.dir(libComposeWasmCompilerPluginsFolder)

    outputs.dir(cachesComposeWasmFolder)

    args(
        kotlinVersion,
        libJVMFolder.asFile.absolutePath,
        cachesComposeWasmFolder,
    )
}

val outputLambdaCacheDir: Provider<Directory> = layout.buildDirectory.dir("incremental-cache")
val buildCacheForLambda by tasks.registering(Exec::class) {
    workingDir = rootDir
    executable = "${project.name}/docker-build-incremental-cache.sh"

    val outputDir = outputLambdaCacheDir

    outputs.dir(outputDir.map { it.dir(cachesComposeWasm) })

    argumentProviders.add {
        listOf(
            lambdaPrefix, // baseDir
            outputDir.get().asFile.normalize().absolutePath, // targetDir
        )
    }
}

val kotlinComposeWasmIc: Configuration by configurations.creating {
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

kotlinComposeWasmIc.outgoing.variants.create("local") {
    attributes {
        attribute(
            CacheAttribute.cacheAttribute,
            CacheAttribute.LOCAL
        )
    }

    artifact(cachesComposeWasmFolder) {
        type = "directory"
        builtBy(runTask)
    }
}

kotlinComposeWasmIc.outgoing.variants.create("lambda") {
    attributes {
        attribute(
            CacheAttribute.cacheAttribute,
            CacheAttribute.LAMBDA
        )
    }

    artifact(outputLambdaCacheDir) {
        type = "directory"
        builtBy(buildCacheForLambda)
    }
}
