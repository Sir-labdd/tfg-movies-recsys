// ============================================================================
// Shared module — DTOs and API contract types (Kotlin Multiplatform)
// ============================================================================
// Two targets:
//   - jvm()         : consumed by :server (Ktor backend on JVM).
//   - js(IR)        : consumed by :composeApp (Compose HTML on browser).
//
// All source code lives under src/commonMain/kotlin/, compiled once
// for each target. The module must remain free of platform-specific
// imports (no java.*, no kotlinx.coroutines schedulers, no
// platform-specific date libraries, etc.).
//
// Versions resolved through the Gradle Version Catalog declared in
// gradle/libs.versions.toml.
// ============================================================================

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.tfg.movies"
version = "0.1.0"

kotlin {
    // ----- Target: JVM (for the :server module) -----
    jvm {
        // Use the same Java toolchain as the rest of the project,
        // so that :server's compileClasspath is consistent.
    }

    // ----- Target: JS (for the :composeApp module) -----
    js(IR) {
        browser()
        // Library, not application — no main entry point needed.
    }

    // ----- Common source set -----
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }

    // Toolchain for the JVM target. Applied at the kotlin{} level
    // so that all JVM compilations within this module use it.
    jvmToolchain(21)
}