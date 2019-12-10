plugins {
  kotlin("jvm")
}

dependencies {
  implementation("junit:junit:4.12")
}

tasks.withType<Jar>().getByName("jar") {
  destinationDirectory.set(File("../lib"))
}