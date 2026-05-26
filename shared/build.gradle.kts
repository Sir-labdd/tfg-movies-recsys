plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.3.0"
}

group = "com.tfg.movies"
version = "0.1.0"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
}

kotlin {
    jvmToolchain(21)
}

// NOTE: this module is currently JVM-only because the only consumer
// is :server. It will be converted to Kotlin Multiplatform (adding
// js(IR) target) when the frontend module is introduced in B7.