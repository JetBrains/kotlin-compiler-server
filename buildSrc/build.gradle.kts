plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

// workaround to pass libs into conventions (see https://github.com/gradle/gradle/issues/15383)
dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}