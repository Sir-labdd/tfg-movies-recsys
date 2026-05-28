// ============================================================================
// Root project configuration
// ============================================================================
// Submodules (server, shared, composeApp) have their own build.gradle.kts
// with their specific configuration.
//
// All plugins used by submodules are declared here with `apply false`,
// which puts them on the build classpath without applying them to any
// project. Submodules then apply them with `alias(libs.plugins.xxx)`
// (no version), which keeps version numbers centralized in the catalog.
//
// This pattern is the standard for Kotlin Multiplatform multi-module
// projects: declaring kotlin.jvm and kotlin.multiplatform in the same
// build requires that both plugins resolve to the same Kotlin version,
// which is only possible if they live in the same classpath.
// ============================================================================

plugins {
    alias(libs.plugins.kotlin.jvm)           apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktor)                  apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}