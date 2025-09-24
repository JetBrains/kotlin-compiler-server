plugins {
    id("base-kotlin-jvm-conventions")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
}