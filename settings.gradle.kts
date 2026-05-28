// ============================================================================
// Project root settings
// ============================================================================
// Toolchain resolver: lets Gradle automatically download the required JDK
// (declared as jvmToolchain(N) in submodules) if it is not already
// installed locally.
//
// Note: this plugin must be declared with its literal version here, because
// the Version Catalog is not yet loaded when settings.gradle.kts is
// evaluated. The same Foojay version is declared in gradle/libs.versions.toml
// for documentation purposes, but is not referenced from this file.
// ============================================================================

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "tfg-movies-recsys"

include(":server")
include(":shared")
include(":composeApp")