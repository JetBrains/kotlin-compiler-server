val kotlinVersion: String by System.getProperties()

plugins {
  kotlin("jvm")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencies {
  implementation("junit:junit:4.13.1")
}

tasks.withType<Jar>().getByName("jar") {
  destinationDirectory.set(File("../$kotlinVersion"))
}