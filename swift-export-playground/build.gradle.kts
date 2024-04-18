plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    // For Analysis API components
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/swift-export-experimental")
}

val kotlinVersion = rootProject.properties["systemProp.kotlinVersion"]
val swiftExportVersion = rootProject.properties["systemProp.swiftExportVersion"]

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
    // For K/N Distribution class
    implementation("org.jetbrains.kotlin:kotlin-native-utils:$kotlinVersion")

    // Analysis API components which are required for the Swift export
    implementation("org.jetbrains.kotlin:analysis-api-standalone-for-ide:$kotlinVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:high-level-api-for-ide:$kotlinVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:high-level-api-fir-for-ide:$kotlinVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:high-level-api-impl-base-for-ide:$kotlinVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:low-level-api-fir-for-ide:$kotlinVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:symbol-light-classes-for-ide:$kotlinVersion") { isTransitive = false }

    // Swift export not-yet-published dependencies.
    implementation("org.jetbrains.kotlin:sir:$swiftExportVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:sir-providers:$swiftExportVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:sir-light-classes:$swiftExportVersion") { isTransitive = false }
    implementation("org.jetbrains.kotlin:sir-printer:$swiftExportVersion") { isTransitive = false }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
}