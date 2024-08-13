plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    // For Analysis API components
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
}

val kotlinVersion = rootProject.properties["systemProp.kotlinVersion"]
val aaVersion = "2.1.0-dev-2515"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler:$aaVersion")

    // Analysis API components which are required for the Swift export
    implementation("org.jetbrains.kotlin:analysis-api-standalone-for-ide:$aaVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:high-level-api-for-ide:$aaVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:high-level-api-fir-for-ide:$aaVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:high-level-api-impl-base-for-ide:$aaVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:low-level-api-fir-for-ide:$aaVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:symbol-light-classes-for-ide:$aaVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:analysis-api-platform-interface-for-ide:$aaVersion") { isTransitive = false }

    // Swift export not-yet-published dependencies.
    implementation("org.jetbrains.kotlin:sir:$kotlinVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:sir-providers:$kotlinVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:sir-light-classes:$kotlinVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:sir-printer:$kotlinVersion") { isTransitive = false }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
}