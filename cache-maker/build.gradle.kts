@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:OptIn(ExperimentalWasmDsl::class)

import org.apache.tools.ant.filters.ConcatFilter
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.COMPILER_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.report.ReportingSettings
import org.jetbrains.kotlin.gradle.targets.js.internal.LibraryFilterCachingService
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.utils.kotlinSessionsDir
import org.jetbrains.kotlin.library.loader.KlibLoader
import java.io.StringReader
import java.nio.file.Files

plugins {
    id("base-kotlin-multiplatform-conventions")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    wasmJs {
        nodejs()
        binaries.executable().forEach { binary: JsIrBinary ->
            binary.linkTask.configure {
                compilerOptions.freeCompilerArgs.add("-Xwasm-included-module-only")
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

val allRuntimes: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    usesPlatformOf(kotlin.wasmJs())
    attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(kotlin.wasmJs()))
    attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
    attributes {
        attribute(
            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
            "compiled-wasm"
        )
    }
}

val allRuntimesKlibs: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    usesPlatformOf(kotlin.wasmJs())
    attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(kotlin.wasmJs()))
    attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
}

// we don't need to build cache-maker
tasks.named("build") {
    dependsOn.clear()
}

val mainCompilation = kotlin.wasmJs().compilations.getByName("main")
val compileTask = mainCompilation.compileTaskProvider

dependencies {
    allRuntimes(libs.bundles.compose)
    allRuntimes(libs.kotlinx.coroutines.core.compose.wasm)
    allRuntimes(libs.kotlin.stdlib.wasm.js)

    allRuntimesKlibs(libs.bundles.compose)
    allRuntimesKlibs(libs.kotlinx.coroutines.core.compose.wasm)
    allRuntimesKlibs(libs.kotlin.stdlib.wasm.js)

    registerTransform(WasmBinaryTransform::class.java) {
        from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "klib")
        to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "compiled-wasm")

        val libraryFilterService = LibraryFilterCachingService.registerIfAbsent(project)


        parameters {
            currentJvmJdkToolsJar.set(
                compileTask.flatMap { it.defaultKotlinJavaToolchain }
                    .flatMap { it.currentJvmJdkToolsJar }
            )
            defaultCompilerClasspath.setFrom(project.configurations.named(COMPILER_CLASSPATH_CONFIGURATION_NAME))
            kotlinPluginVersion.set(
                compileTask.map { getKotlinPluginVersion(it.logger) }
            )
            pathProvider.set(
                compileTask.map { it.path }
            )
            projectRootFile.set(
                project.projectDir
            )
            val projectName = project.name
            this.projectName.set(projectName)
            projectSessionsDir.set(project.kotlinSessionsDir)

            this.buildDir.set(project.layout.buildDirectory.asFile)

            libraryFilterCacheService.set(libraryFilterService)

            compilerOptions.set(
                compileTask.map {
                    val args = K2JSCompilerArguments()
                    KotlinCommonCompilerOptionsHelper.fillCompilerArguments(it.compilerOptions, args)
                    args
                }
            )
            enhancedFreeCompilerArgs.set(compileTask.flatMap { it.enhancedFreeCompilerArgs })

            classpath.from(
                allRuntimesKlibs
            )

//            BinaryenPlugin.apply(project)
//            it.binaryenExec.set(project.extensions.findByType(BinaryenEnvSpec::class.java).executable)
//            it.mode.set(binary.mode)
        }
    }

    attributesSchema {
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE) {
            compatibilityRules.add(JarToKlibRule::class)
        }
    }
}

val prepareRuntime by tasks.registering(Copy::class) {
    val allRuntimes: FileCollection = allRuntimes

    from(allRuntimes) {
        include {
            it.name.endsWith(".mjs") || it.name.endsWith(".wasm")
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
                    System.getenv(STATIC_URL_ENV_VAR) ?: localhostStaticUrl
                )

                outputDir.get().asFile.resolve("$nameWithoutExtension.mjs").writeText(resultMjs)
            }

        fs.copy {
            from(inputDir) {
                include {
                    println(it.name)
                    it.name.substringBeforeLast("-") == "skiko"
                }
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

class JarToKlibRule : AttributeCompatibilityRule<String> {
    // Implement the execute method which will check compatibility
    override fun execute(details: CompatibilityCheckDetails<String>) {
        // Switch case to check the consumer value for supported Java versions

        if (details.producerValue == "jar" && details.consumerValue == "klib") {
            details.compatible()
        }
    }
}

@CacheableTransform
internal abstract class WasmBinaryTransform : TransformAction<WasmBinaryTransform.Parameters> {
    /**
     * Parameters for the [WasmBinaryTransform].
     */
    abstract class Parameters : TransformParameters {
        @get:Internal
        abstract val compilerOptions: Property<K2JSCompilerArguments>

        @get:Internal
        abstract val currentJvmJdkToolsJar: Property<File>

        @get:Classpath
        abstract val defaultCompilerClasspath: ConfigurableFileCollection

        @get:Input
        abstract val kotlinPluginVersion: Property<String>

        @get:Internal
        abstract val pathProvider: Property<String>

        @get:Internal
        abstract val projectRootFile: Property<File>

        @get:Internal
        internal abstract val projectName: Property<String>

        @get:Internal
        internal abstract val projectSessionsDir: DirectoryProperty

        @get:Internal
        abstract val buildDir: Property<File>

        @get:Internal
        internal abstract val libraryFilterCacheService: Property<LibraryFilterCachingService>

        @get:Input
        internal abstract val enhancedFreeCompilerArgs: ListProperty<String>

        @get:Classpath
        internal abstract val classpath: ConfigurableFileCollection

        @get:Internal
        internal abstract val binaryenExec: Property<String>
    }

    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val execOps: ExecOperations

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputArtifact: Provider<FileSystemLocation>

    @get:CompileClasspath
    @get:InputArtifactDependencies
    abstract val dependencies: FileCollection

    override fun transform(outputs: TransformOutputs) {
        val inputFile = inputArtifact.get().asFile

        val compilerOutputDir = Files.createTempDirectory("wasm-transform-").toFile()

        val isKotlinLibrary = isKotlinLibrary(inputFile)

        if (!isKotlinLibrary) {
            fs.copy {
                from(archiveOperations.zipTree(inputFile))
                into(outputs.dir(inputFile.name.replaceAfterLast(".", "-transformed")))
            }
            return
        }

        val args: K2JSCompilerArguments = parameters.compilerOptions.get()
        args.apply {
            this.outputDir = compilerOutputDir.absolutePath
            moduleName = inputFile.nameWithoutExtension
            includes = inputFile.absolutePath
            wasmIncludedModuleOnly = true
            irProduceJs = true
            libraries = parameters.classpath.files.plus(inputFile).joinToString(File.pathSeparator) { it.absolutePath }
        }

        args.freeArgs += parameters.enhancedFreeCompilerArgs.get()

        val workArgs = GradleKotlinCompilerWorkArguments(
            projectFiles = ProjectFilesForCompilation(
                parameters.projectRootFile.get(),
                GradleCompilerRunner.getOrCreateClientFlagFile(LOGGER, parameters.projectName.get()),
                GradleCompilerRunner.getOrCreateSessionFlagFile(LOGGER, parameters.projectSessionsDir.get().asFile),
                parameters.buildDir.get(),
            ),
            compilerFullClasspath = (parameters.defaultCompilerClasspath.files + parameters.currentJvmJdkToolsJar.orNull).filterNotNull(),
            compilerClassName = KotlinCompilerClass.JS,
            compilerArgs = ArgumentUtils.convertArgumentsToStringList(args).toTypedArray(),
            isVerbose = false,
            incrementalCompilationEnvironment = null,
            incrementalModuleInfo = null,
            outputFiles = emptyList(),
            taskPath = parameters.pathProvider.get(),
            reportingSettings = ReportingSettings(),
            kotlinScriptExtensions = emptyArray(),
            allWarningsAsErrors = false,
            compilerExecutionSettings = CompilerExecutionSettings(
                null,
                KotlinCompilerExecutionStrategy.DAEMON,
                true,
                generateCompilerRefIndex = false,
            ),
            errorsFiles = null,
            kotlinPluginVersion = parameters.kotlinPluginVersion.get(),
            //no need to log warnings in MessageCollector hear it will be logged by compiler
            kotlinLanguageVersion = args.languageVersion?.let { v ->
                KotlinVersion.fromVersion(
                    v
                )
            } ?: KotlinVersion.DEFAULT,
            compilerArgumentsLogLevel = KotlinCompilerArgumentsLogLevel.DEFAULT,
        )

        GradleKotlinCompilerWork(
            workArgs
        ).run()

        val binaryenOutputDirectory = outputs.dir(inputFile.name.replace(".klib", "-transformed"))

//        execOps.exec {
//            executable = parameters.binaryenExec.get()
//            val inputFileBinaryen = compilerOutputDir.listFiles().first { it.extension == "wasm" }
//            val newArgs = mutableListOf<String>()
//            newArgs.addAll(binaryenArgs(false))
//            newArgs.add(inputFileBinaryen.absolutePath)
//            newArgs.add("-o")
//            newArgs.add(binaryenOutputDirectory.resolve(inputFileBinaryen.name).absolutePath)
//            setWorkingDir(binaryenOutputDirectory)
//            args = newArgs
//        }

//        binaryenOutputDirectory.mkdirs()
//        binaryenOutputDirectory.resolve(mainMjs.name).writeText(resultMjs)

        fs.copy {
            from(compilerOutputDir)
            into(binaryenOutputDirectory)
            include("*.mjs", "*.js", "*.js.map", "*.wasm")
        }

    }

    private fun isKotlinLibrary(file: File): Boolean {
        return parameters.libraryFilterCacheService.get().getOrCompute(
            LibraryFilterCachingService.LibraryFilterCacheKey(
                file
            )
        ) {
            KlibLoader { libraryPaths(it.absolutePath) }.load().librariesStdlibFirst.isNotEmpty()
        }
    }

    private companion object {
        private val LOGGER: Logger = Logging.getLogger(WasmBinaryTransform::class.java)
    }
}