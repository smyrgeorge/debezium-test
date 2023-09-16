import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
}

group = rootProject.group
version = rootProject.version

java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    // IMPORTANT: must be last.
    mavenLocal()
}

dependencies {
    // Kafka
    // https://mvnrepository.com/artifact/org.apache.kafka/connect-api
    implementation("org.apache.kafka:connect-api:3.5.1")
    implementation("org.apache.kafka:connect-transforms:3.5.1")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.0")

    // Reflexion
    implementation("org.reflections:reflections:0.10.2")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")

    // https://github.com/mockito/mockito-kotlin
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        // Use "-Xcontext-receivers" to enable context receivers.
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    // Log each test.
    testLogging { events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED) }

    // Print a summary after test suite.
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            // Wll match the outermost suite.
            if (suite.parent == null) {
                println("\nTest result: ${result.resultType}")
                val summary = "Test summary: ${result.testCount} tests, " +
                        "${result.successfulTestCount} succeeded, " +
                        "${result.failedTestCount} failed, " +
                        "${result.skippedTestCount} skipped"
                println(summary)
            }
        }
    })
}

application {
    mainClass.set("io.smyrgeorge.test.Main")
}

tasks.withType<Jar> {
    archiveClassifier.set("")
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("fat")
}
