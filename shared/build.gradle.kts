// ============================================================================
// Shared module — DTOs and API contract types
// ============================================================================
// Currently configured as kotlin("jvm") because the only consumer is the
// :server module. Will be converted to Kotlin Multiplatform (adding the
// js(IR) target) in the next sub-step of block B7, when the :composeApp
// frontend module is introduced and needs to consume the same DTOs.
//
// Versions resolved through the Gradle Version Catalog declared in
// gradle/libs.versions.toml.
// ============================================================================

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.tfg.movies"
version = "0.1.0"

dependencies {
    implementation(libs.kotlinx.serialization.json)
}

kotlin {
    jvmToolchain(21)
}