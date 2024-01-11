import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate

val kotlinVersion: String by System.getProperties()
val kotlinIdeVersion: String by System.getProperties()
val kotlinIdeVersionSuffix: String by System.getProperties()
val indexes: String by System.getProperties()
val indexesJs: String by System.getProperties()
val indexesWasm: String by System.getProperties()

val libJS = "$kotlinVersion-js"
val libWasm = "$kotlinVersion-wasm"
val libCompilerPlugins = "$kotlinVersion-compiler-plugins"
val libJVM = kotlinVersion

val Project.libJSFolder
    get() = rootProject.layout.projectDirectory.dir(libJS)

val Project.libWasmFolder
    get() = rootProject.layout.projectDirectory.dir(libWasm)

val Project.libCompilerPluginsFolder
    get() = rootProject.layout.projectDirectory.dir(libCompilerPlugins)

val Project.libJVMFolder
    get() = rootProject.layout.projectDirectory.dir(libJVM)