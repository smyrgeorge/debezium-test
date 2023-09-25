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
    maven { url = uri("https://packages.confluent.io/maven/") }
    // IMPORTANT: must be last.
    mavenLocal()
}

val protocVersion: String by rootProject.extra
val jacksonVersion: String by rootProject.extra
val mockitoVersion: String by rootProject.extra
val kafkaConnectVersion: String by rootProject.extra
val kafkaConfluentVersion: String by rootProject.extra

dependencies {
    // Kafka
    // https://mvnrepository.com/artifact/org.apache.kafka/connect-api
    implementation("org.apache.kafka:connect-api:$kafkaConnectVersion")
    // https://packages.confluent.io/maven/io/confluent/kafka-schema-registry-client/
    implementation("io.confluent:kafka-schema-registry-client:$kafkaConfluentVersion")
    implementation("io.confluent:kafka-protobuf-serializer:$kafkaConfluentVersion")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Protobuf
    implementation("com.google.protobuf:protobuf-java:$protocVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protocVersion")

    // https://github.com/mockito/mockito-kotlin
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoVersion")
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
