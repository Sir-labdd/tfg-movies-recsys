// ============================================================================
// Server module — Ktor backend
// ============================================================================
// Versions and dependency coordinates are resolved through the Gradle
// Version Catalog declared in gradle/libs.versions.toml. This keeps
// version numbers in a single file shared across all modules.
// ============================================================================

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    application
}

group = "com.tfg.movies"
version = "0.1.0"

application {
    mainClass.set("com.tfg.movies.server.ApplicationKt")
}

dependencies {
    // ---- Shared module (DTOs) ----
    implementation(project(":shared"))

    // ---- Ktor server ----
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.config.yaml)

    // ---- Database access (JDBC + connection pool) ----
    implementation(libs.postgresql)
    implementation(libs.hikari)

    // ---- Logging ----
    implementation(libs.logback.classic)

    // ---- Tests ----
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}