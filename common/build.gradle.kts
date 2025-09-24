plugins {
    id("base-kotlin-jvm-conventions")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:${libs.versions.kotlin.get()}")
}

