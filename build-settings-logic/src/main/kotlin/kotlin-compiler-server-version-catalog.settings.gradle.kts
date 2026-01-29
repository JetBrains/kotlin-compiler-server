pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()

        val additionalRepositoryProperty = providers.gradleProperty("kotlin_repo_url")
        if (additionalRepositoryProperty.isPresent) {
            maven(additionalRepositoryProperty.get()) {
                name = "KotlinDevRepo"
            }
            logger.info("A custom Kotlin repository ${additionalRepositoryProperty.get()} was added")
        }
        maven("https://redirector.kotlinlang.org/maven/dev")
        mavenLocal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()

        val additionalRepositoryProperty = providers.gradleProperty("kotlin_repo_url")
        if (additionalRepositoryProperty.isPresent) {
            maven(additionalRepositoryProperty.get()) {
                name = "KotlinDevRepo"
            }
            logger.info("A custom Kotlin repository ${additionalRepositoryProperty.get()} was added")
        }
        maven("https://redirector.kotlinlang.org/maven/dev")
        mavenLocal()
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