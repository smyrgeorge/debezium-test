group = "io.smyrgeorge.test"
version = "0.1.0"

val protocVersion: String by extra { "3.24.3" }
val jacksonVersion: String by extra { "2.15.2" }
val mockitoVersion: String by extra { "5.1.0" }
val kafkaConnectVersion: String by extra { "3.5.1" }
val kafkaConfluentVersion: String by extra { "7.5.0" }

// Common plugin versions here.
plugins {
    // NOTE: we use [apply] false.
    // https://docs.gradle.org/current/userguide/plugins.html#sec:subprojects_plugins_dsl
    // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm
    kotlin("jvm") version "1.9.0" apply false
    // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.spring
    kotlin("plugin.spring") version "1.9.0" apply false
    // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization
    kotlin("plugin.serialization") version "1.9.0" apply false
    // https://plugins.gradle.org/plugin/org.springframework.boot
    id("org.springframework.boot") version "3.1.2" apply false
    // https://plugins.gradle.org/plugin/io.spring.dependency-management
    id("io.spring.dependency-management") version "1.1.3" apply false
    // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}