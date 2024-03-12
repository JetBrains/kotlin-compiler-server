plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-idea:$kotlinVersion")
    implementation("org.jetbrains.kotlin:base-fe10-analysis:231-$kotlinIdeVersion-$kotlinIdeVersionSuffix")
}
