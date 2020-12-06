val kotlinVersion: String by System.getProperties()

plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
}