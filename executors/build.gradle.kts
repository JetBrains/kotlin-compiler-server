val kotlinVersion: String by System.getProperties()

plugins {
  kotlin("jvm")
}

kotlin.jvmToolchain(11)

dependencies {
  implementation("junit:junit:4.13.2")
}

tasks.withType<Jar>().getByName("jar") {
  destinationDirectory.set(File("../$kotlinVersion"))
}
