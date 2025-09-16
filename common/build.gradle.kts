plugins {
    id("base-kotlin-jvm-conventions")
}

dependencies {
    implementation(libs.kotlin.gradle.plugin.idea)
    implementation(libs.kotlin.base.fe10.analysis)
}
