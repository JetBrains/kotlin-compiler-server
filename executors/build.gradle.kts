plugins {
  kotlin("jvm")
}

dependencies {
  implementation("junit:junit:4.12")
  implementation(kotlin("stdlib"))
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.+")
}

tasks.withType<Jar>().getByName("jar") {
  destinationDirectory.set(File("../lib"))
}