dependencyResolutionManagement {
    versionCatalogs {
        register("libs").configure {
            val kotlinVersion = providers.gradleProperty("kotlinVersion")
            if (kotlinVersion.isPresent) {
                version("kotlin", kotlinVersion.get())
            }
        }
    }
}