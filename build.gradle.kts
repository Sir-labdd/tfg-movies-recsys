// Root project configuration.
// Submodules (server, shared, composeApp) have their own build.gradle.kts
// with their specific configuration.

plugins {
    // Define versions of common plugins here so submodules can use them
    // without repeating versions.
    kotlin("jvm") version "2.3.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}