plugins {
    id("base-kotlin-jvm-conventions")
}

dependencies {
    implementation(libs.aws.lambda.core)
    implementation(libs.aws.lambda.sdk)
    implementation(libs.lettuce.core)
    implementation(libs.slf4j.simple)
    implementation(libs.jackson.module.kotlin)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.mockito)
    testImplementation(libs.testcontainers)
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

val buildProxyLambda by tasks.creating(Zip::class) {
    dependsOn(tasks.compileKotlin)
    from(tasks.compileKotlin.flatMap { it.destinationDirectory })
    from(tasks.processResources)
    into("lib") {
        from(configurations.runtimeClasspath)
    }
}
