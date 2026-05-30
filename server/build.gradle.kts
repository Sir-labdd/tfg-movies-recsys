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

// ============================================================================
// Copy the production frontend bundle into the server's classpath so
// Ktor can serve it as static files. The bundle lives under the
// "static" resource directory, served at "/" by the static route.
//
// Task dependency chain:
//   :composeApp:jsBrowserProductionWebpack  (build the JS bundle)
//   → copyFrontend                          (copy to server resources)
//   → :server:classes                       (include in classpath)
//   → :server:run                           (start with frontend)
// ============================================================================
val copyFrontend by tasks.registering(Copy::class) {
    dependsOn(":composeApp:jsBrowserProductionWebpack")
    mustRunAfter(tasks.named("processResources"))

    // JS bundles from webpack production output.
    from(project(":composeApp").layout.buildDirectory.dir("kotlin-webpack/js/productionExecutable")) {
        include("*.js", "*.js.map")
    }
    // index.html from processed resources.
    from(project(":composeApp").layout.buildDirectory.dir("processedResources/js/main")) {
        include("index.html")
    }

    into(layout.buildDirectory.dir("resources/main/static"))
}

tasks.named("classes") {
    dependsOn(copyFrontend)
}