plugins {
  kotlin("jvm")
}

kotlin.jvmToolchain {
  languageVersion.set(JavaLanguageVersion.of(17))
  vendor.set(JvmVendorSpec.AMAZON)
}

dependencies {
  implementation(libs.junit)
}

tasks.withType<Jar>().getByName("jar") {
  destinationDirectory.set(libJVMFolder)
}