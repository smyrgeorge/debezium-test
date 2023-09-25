import com.google.protobuf.gradle.id
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.serialization")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    // https://github.com/google/protobuf-gradle-plugin
    id("com.google.protobuf") version "0.9.4"
}

group = rootProject.group
version = rootProject.version

java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
    // IMPORTANT: must be last.
    mavenLocal()
}

val protocVersion: String by rootProject.extra

dependencies {
    // Internal dependencies
    implementation(project(":sample-kotlin-domain"))

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Spring boot
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Kafka
    // https://github.com/reactor/reactor-kafka
    implementation("io.projectreactor.kafka:reactor-kafka:1.3.20")
    implementation("io.confluent:kafka-protobuf-serializer:7.5.0")
    implementation("io.confluent:kafka-schema-registry-client:7.5.0")
    implementation("io.confluent:kafka-protobuf-provider:7.5.0")

    // Protobuf
    implementation("com.google.protobuf:protobuf-kotlin:$protocVersion")

    // https://mvnrepository.com/artifact/org.javers/javers-core
    implementation("org.javers:javers-core:7.3.2")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // https://github.com/mockito/mockito-kotlin
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        // Use "-Xcontext-receivers" to enable context receivers.
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
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

// Disables the build of '**-plain.jar'
tasks.getByName<Jar>("jar") {
    enabled = false
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protocVersion"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins { id("kotlin") }
        }
    }
}