val kotlinVersion: String by System.getProperties()
val kotlinIdeVersion: String by System.getProperties()

plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
    implementation("org.jetbrains.kotlin:common:222-$kotlinIdeVersion-IJ4167.29")
}
