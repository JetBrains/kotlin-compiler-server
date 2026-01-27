plugins {
    id("base-kotlin-jvm-conventions")
}

dependencies {
    implementation(libs.kotlin.compiler.embeddable)
    implementation(libs.jackson.module.kotlin)
}