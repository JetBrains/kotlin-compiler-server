plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":common", configuration = "default"))
}

application {
    mainClass.set("cache.MainKt")
}

val runTask = tasks.named<JavaExec>("run") {
    dependsOn(":dependencies:copyDependencies")
    dependsOn(":dependencies:copyWasmDependencies")
    dependsOn(":dependencies:copyComposeWasmCompilerPlugins")
    dependsOn(":dependencies:copyComposeWasmDependencies")

    val rootName = project.rootProject.projectDir.toString()

    val kotlinVersion = libs.versions.kotlin.get()
    inputs.property("kotlinVersion", kotlinVersion)

    // Adding classpath directories as task input for up-to-date checks
    inputs.dir(libWasmFolder)
    inputs.dir(libComposeWasmFolder)
    inputs.dir(libComposeWasmCompilerPluginsFolder)

    // Adding resulting index files as output for up-to-date checks
    val composeCacheComposeWasm = "$rootName${File.separator}$cachesComposeWasm"
    outputs.dir(cachesComposeWasmFolder)

    args = listOf(
        kotlinVersion,
        libJVMFolder.asFile.absolutePath,
        composeCacheComposeWasm,
    )
}

val outputLocalCacheDir = rootDir.resolve(cachesComposeWasm)

val outputLambdaCacheDir: Provider<Directory> = layout.buildDirectory.dir("incremental-cache")
val buildCacheForLambda by tasks.registering(Exec::class) {
    workingDir = rootDir
    executable = "${project.name}/docker-build-incremental-cache.sh"

    val outputDir = outputLambdaCacheDir

    outputs.dir(outputDir.map { it.dir(cachesComposeWasm) })

    doFirst {
        args = listOf(
            lambdaPrefix, // baseDir
            outputDir.get().asFile.normalize().absolutePath, // targetDir
        )
    }
}

val kotlinComposeWasmIcLocalCache: Configuration by configurations.creating {
    isTransitive = false
    isCanBeResolved = false
    isCanBeConsumed = true
    attributes {
        attribute(
            CacheAttribute.cacheAttribute,
            CacheAttribute.LOCAL
        )
    }
}

val kotlinComposeWasmIcLambdaCache: Configuration by configurations.creating {
    isTransitive = false
    isCanBeResolved = false
    isCanBeConsumed = true
    attributes {
        attribute(
            CacheAttribute.cacheAttribute,
            CacheAttribute.LAMBDA
        )
    }
}

artifacts.add(kotlinComposeWasmIcLocalCache.name, outputLocalCacheDir) {
    builtBy(runTask)
}

artifacts.add(kotlinComposeWasmIcLambdaCache.name, outputLambdaCacheDir) {
    builtBy(buildCacheForLambda)
}

