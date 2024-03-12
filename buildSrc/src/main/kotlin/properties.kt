import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate

val kotlinVersion: String by System.getProperties()
val kotlinIdeVersion: String by System.getProperties()
val kotlinIdeVersionSuffix: String by System.getProperties()
val indexes: String by System.getProperties()
val indexesJs: String by System.getProperties()
val indexesWasm: String by System.getProperties()
val indexesComposeWasm: String by System.getProperties()

val libJS = "$kotlinVersion-js"
val libWasm = "$kotlinVersion-wasm"
val libComposeWasm = "$kotlinVersion-compose-wasm"
val libComposeWasmCompilerPlugins = "$kotlinVersion-compose-wasm-compiler-plugins"
val libJVM = kotlinVersion

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