val kotlinVersion: String by System.getProperties()
val kotlinIdeVersion: String by System.getProperties()
val kotlinIdeVersionSuffix: String by System.getProperties()

plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-idea:$kotlinVersion")
    implementation("org.jetbrains.kotlin:base-fe10-analysis:223-$kotlinIdeVersion-$kotlinIdeVersionSuffix")
}
