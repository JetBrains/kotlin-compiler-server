plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    // For Analysis API components
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
}

dependencies {
    implementation(libs.kotlin.compiler)

    // Analysis API components which are required for the Swift export
    implementation(libs.analysis.api.standalone.`for`.ide) { isTransitive = false }
    implementation(libs.high.level.api.`for`.ide) { isTransitive = false }
    implementation(libs.high.level.api.fir.`for`.ide) { isTransitive = false }
    implementation(libs.high.level.api.impl.base.`for`.ide) { isTransitive = false }
    implementation(libs.low.level.api.fir.`for`.ide) { isTransitive = false }
    implementation(libs.symbol.light.classes.`for`.ide) { isTransitive = false }
    implementation(libs.analysis.api.platform.`interface`.`for`.ide) { isTransitive = false }
    implementation(libs.caffeine)

    // Swift export not-yet-published dependencies.
    implementation(libs.sir) { isTransitive = false }
    implementation(libs.sir.providers) { isTransitive = false }
    implementation(libs.sir.light.classes) { isTransitive = false }
    implementation(libs.sir.printer) { isTransitive = false }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
}