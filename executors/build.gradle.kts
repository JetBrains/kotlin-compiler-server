plugins {
  kotlin("jvm")
}

dependencies {
  implementation("junit:junit:4.12")
  implementation(kotlin("stdlib"))
}

tasks.withType<Jar>().getByName("jar") {
  destinationDirectory.set(File("../lib"))
}