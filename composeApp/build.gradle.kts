// ============================================================================
// composeApp module — Compose HTML frontend (browser JS target)
// ============================================================================
// This module builds the web client of the application. It is a Kotlin
// Multiplatform module with a single target: js(IR) compiled for the
// browser. The UI is built with Compose HTML (org.jetbrains.compose.html),
// not Compose Material 3, because the deliverable is a DOM-based web
// application — see section 7.3.5 of the memoir for the rationale.
//
// Source layout (Kotlin Multiplatform convention):
//   src/jsMain/kotlin/       Kotlin sources compiled to JavaScript
//   src/jsMain/resources/    Static resources packaged with the bundle
//                            (notably index.html, served by the dev server)
//
// Versions resolved through the Gradle Version Catalog declared in
// gradle/libs.versions.toml.
// ============================================================================

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

group = "com.tfg.movies"
version = "0.1.0"

kotlin {
    js(IR) {
        browser {
            // The dev server runs on this port during
            // `./gradlew :composeApp:jsBrowserDevelopmentRun`.
            // It is separate from the Ktor server (port 8080); we will
            // wire them together in a later sub-step (B7.8).
            commonWebpackConfig {
                cssSupport { enabled.set(true) }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                // Shared DTOs from the :shared module (via the js target
                // we enabled when converting :shared to Multiplatform).
                implementation(project(":shared"))

                // Compose HTML: provides Div, Text, StyleSheet, etc.
                // We declare these via the catalog (not via the `compose`
                // accessor, which was deprecated in Compose 1.10).
                implementation(libs.compose.runtime)
                implementation(libs.compose.html.core)

                // Ktor client to call the backend API.
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.js)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)

                // Coroutines (used by Ktor client and by Compose effects).
                implementation(libs.kotlinx.coroutines.core)

                // JSON serialization for the API client code.
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}