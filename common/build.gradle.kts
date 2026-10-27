plugins {
    id("base-kotlin-jvm-conventions")
}

dependencies {
    implementation(libs.kotlin.util.klib)
    implementation(libs.jackson.module.kotlin)
}