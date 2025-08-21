plugins {
    kotlin("jvm")
}

kotlin {

    logger.info("For the ${project.name} we used $kotlinVersion kotlin version in this build.")

    sourceSets.configureEach {
        languageSettings {
            val kotlinLanguageVersion = project.providers.gradleProperty("kotlin_language_version")
            if (kotlinLanguageVersion.isPresent) {
                languageVersion = kotlinLanguageVersion.get()
                logger.info("An overriding Kotlin language version of $languageVersion was found for project ${project.name}")
            }
            val kotlinApiVersion = project.providers.gradleProperty("kotlin_api_version")
            if (kotlinApiVersion.isPresent) {
                apiVersion = kotlinApiVersion.get()
                logger.info("An overriding Kotlin api version of $apiVersion was found for project ${project.name}")
            }
        }
    }

    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.AMAZON)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {

        freeCompilerArgs.addAll(
            "-Xreport-all-warnings",
            "-Xrender-internal-diagnostic-names",
        )

        allWarningsAsErrors.set(false)

        // Adding additional cli options for testing purpose
        project.providers.gradleProperty("kotlin_additional_cli_options").orNull?.let { options ->
            options.split(" ").filter { it.isNotBlank() }.forEach {
                freeCompilerArgs.add(it)
            }
        }
    }
}
