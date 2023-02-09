import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary

plugins {
    kotlin("multiplatform")
}

val kotlinJsDependency by rootProject.configurations

kotlin {
    js(IR) {
        nodejs()
        val executables = binaries.executable()
        val main by compilations.getting
        main.configurations.apiConfiguration.extendsFrom(kotlinJsDependency)

        val jsCaches by configurations.creating {
            isVisible = false
            isCanBeResolved = false
            isCanBeConsumed = true

            attributes {
                attribute(
                    KotlinPlatformType.attribute,
                    KotlinPlatformType.js
                )
                attribute(
                    KotlinJsCompilerAttribute.jsCompilerAttribute,
                    KotlinJsCompilerAttribute.ir
                )
                attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    objects.named(LibraryElements::class.java, "js-ir-cache")
                )
            }
        }

        val linkTask = executables
            .filterIsInstance<JsIrBinary>()
            .single { it.mode == KotlinJsBinaryMode.DEVELOPMENT }
            .linkTask

        val rootCacheDir = linkTask
            .map { it.rootCacheDirectory }

        artifacts.add(jsCaches.name, rootCacheDir) {
            builtBy(linkTask)
        }
    }
}

// just to calm down root project's allprojects block
configurations.register("implementation")