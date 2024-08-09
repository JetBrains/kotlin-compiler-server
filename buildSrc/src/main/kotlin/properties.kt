import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.the

val indexes: String by System.getProperties()
val indexesJs: String by System.getProperties()
val indexesWasm: String by System.getProperties()
val indexesComposeWasm: String by System.getProperties()

// workaround to pass libs into conventions (see https://github.com/gradle/gradle/issues/15383)
val Project.kotlinVersion
    get() = the<LibrariesForLibs>().versions.kotlin.get()

val Project.libJS
    get() = "$kotlinVersion-js"
val Project.libWasm
    get() = "$kotlinVersion-wasm"
val Project.libComposeWasm
    get() = "$kotlinVersion-compose-wasm"
val Project.libComposeWasmCompilerPlugins
    get() = "$kotlinVersion-compose-wasm-compiler-plugins"
val Project.libJVM
    get() = kotlinVersion

val Project.libJSFolder
    get() = rootProject.layout.projectDirectory.dir(libJS)

val Project.libWasmFolder
    get() = rootProject.layout.projectDirectory.dir(libWasm)

val Project.libComposeWasmFolder
    get() = rootProject.layout.projectDirectory.dir(libComposeWasm)

val Project.libComposeWasmCompilerPluginsFolder
    get() = rootProject.layout.projectDirectory.dir(libComposeWasmCompilerPlugins)

val Project.libJVMFolder
    get() = rootProject.layout.projectDirectory.dir(libJVM)