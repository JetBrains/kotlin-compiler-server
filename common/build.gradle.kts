val kotlinVersion: String by System.getProperties()
val kotlinIdeVersion: String by System.getProperties()

plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
    implementation("org.jetbrains.kotlin:common:222-$kotlinIdeVersion-IJ4167.29")
    implementation("org.jetbrains.kotlin:base-fe10-analysis:222-$kotlinIdeVersion-IJ4167.29")
}
