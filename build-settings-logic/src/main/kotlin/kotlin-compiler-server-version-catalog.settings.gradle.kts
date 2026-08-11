pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()

        val kotlinRepoUrl = providers.gradleProperty("kotlin_repo_url")
        val kotlinVersion = providers.gradleProperty("kotlin_version")
        if (!kotlinRepoUrl.orNull.isNullOrEmpty() && !kotlinVersion.orNull.isNullOrEmpty()) {
            exclusiveContent {
                forRepository {
                    maven(kotlinRepoUrl.get()) {
                        name = "KotlinDevRepo"
                    }
                }
                filter {
                    includeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", kotlinVersion.get())
                }
            }
            logger.info("A custom Kotlin repository ${kotlinRepoUrl.get()} was added")
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

        val kotlinRepoUrl = providers.gradleProperty("kotlin_repo_url")
        val kotlinVersion = providers.gradleProperty("kotlin_version")
        if (!kotlinRepoUrl.orNull.isNullOrEmpty() && !kotlinVersion.orNull.isNullOrEmpty()) {
            exclusiveContent {
                forRepository {
                    maven(kotlinRepoUrl.get()) {
                        name = "KotlinDevRepo"
                    }
                }
                filter {
                    includeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", kotlinVersion.get())
                }
            }
            logger.info("A custom Kotlin repository ${kotlinRepoUrl.get()} was added")
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