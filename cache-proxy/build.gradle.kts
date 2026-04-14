plugins {
    id("base-kotlin-jvm-conventions")
}

dependencies {
    implementation(libs.aws.lambda.core)
    implementation(libs.aws.lambda.sdk)
    implementation(libs.lettuce.core)
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation(libs.jackson.module.kotlin)

    testImplementation(libs.kotlin.test)
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
