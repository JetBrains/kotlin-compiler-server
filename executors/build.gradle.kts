import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
}

java.sourceCompatibility = JavaVersion.VERSION_17

dependencies {
  implementation("junit:junit:4.13.2")
}

tasks.withType<KotlinCompile> {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

tasks.withType<Jar>().getByName("jar") {
  destinationDirectory.set(libJVMFolder)
}