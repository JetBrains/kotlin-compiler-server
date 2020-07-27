val kotlinVersion: String by System.getProperties()

plugins {
  kotlin("jvm")
}

dependencies {
  implementation("junit:junit:4.13")
}

tasks.withType<Jar>().getByName("jar") {
  destinationDirectory.set(File("../$kotlinVersion"))
}