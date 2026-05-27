plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.3.0"
    id("io.ktor.plugin") version "3.4.3"
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
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-config-yaml")

    // ---- Database access (JDBC + connection pool) ----
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:6.2.1")

    // ---- Logging ----
    implementation("ch.qos.logback:logback-classic:1.5.12")

    // ---- Tests ----
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("io.ktor:ktor-client-content-negotiation")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

kotlin {
    jvmToolchain(21)
}