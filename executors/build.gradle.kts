plugins {
  id("base-kotlin-jvm-conventions")
}

dependencies {
  implementation(libs.junit)
  implementation(libs.jackson.module.kotlin)
}

tasks.withType<Jar>().getByName("jar") {
  destinationDirectory.set(libJVMFolder)
}