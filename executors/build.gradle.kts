plugins {
  kotlin("jvm")
}

kotlin.jvmToolchain {
  languageVersion.set(JavaLanguageVersion.of(17))
  vendor.set(JvmVendorSpec.ADOPTIUM)
}

dependencies {
  implementation("junit:junit:4.13.2")
}

tasks.withType<Jar>().getByName("jar") {
  destinationDirectory.set(libJVMFolder)
}