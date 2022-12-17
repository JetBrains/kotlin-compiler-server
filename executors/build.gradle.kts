val kotlinVersion: String by System.getProperties()

plugins {
  kotlin("jvm")
}

java.sourceCompatibility = JavaVersion.VERSION_11

dependencies {
  implementation("junit:junit:4.13.2")
}

tasks.withType<Jar>().getByName("jar") {
  destinationDirectory.set(File("../$kotlinVersion"))
}