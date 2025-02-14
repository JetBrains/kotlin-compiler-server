plugins {
  id("base-kotlin-jvm-conventions")
}

dependencies {
  implementation(libs.junit)
}

tasks.withType<Jar>().getByName("jar") {
  destinationDirectory.set(libJVMFolder)
}