pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        maven("https://redirector.kotlinlang.org/maven/dev")

        val additionalRepositoryProperty = providers.gradleProperty("kotlin_repo_url")
        if (additionalRepositoryProperty.isPresent) {
            maven(additionalRepositoryProperty.get()) {
                name = "KotlinDevRepo"
            }
            logger.info("A custom Kotlin repository ${additionalRepositoryProperty.get()} was added")
        }
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://redirector.kotlinlang.org/maven/dev")

        val additionalRepositoryProperty = providers.gradleProperty("kotlin_repo_url")
        if (additionalRepositoryProperty.isPresent) {
            maven(additionalRepositoryProperty.get()) {
                name = "KotlinDevRepo"
            }
            logger.info("A custom Kotlin repository ${additionalRepositoryProperty.get()} was added")
        }
    }

    versionCatalogs {
        register("libs").configure {
            val kotlinVersion = providers.gradleProperty("kotlin_version")
            if (kotlinVersion.isPresent) {
                version("kotlin", kotlinVersion.get())
            }
        }
    }
}