dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
        val kotlinVersion = providers.gradleProperty("kotlinVersion")
        if (kotlinVersion.isPresent) {
            create("libs") {
                version("kotlin", kotlinVersion.get())
            }
        }
    }
}