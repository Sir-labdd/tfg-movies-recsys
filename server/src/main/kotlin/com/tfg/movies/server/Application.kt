package com.tfg.movies.server

import com.tfg.movies.server.config.DatabaseFactory
import com.tfg.movies.server.health.healthRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*

/**
 * Entry point for the TFG Movies backend.
 *
 * Reads configuration from src/main/resources/application.yaml.
 * The server listens on the port defined in that file (default 8080).
 */
fun main(args: Array<String>) {
    EngineMain.main(args)
}

/**
 * Application module. Wired automatically by Ktor via application.yaml.
 *
 * Configures plugins, initializes the database connection pool, and
 * registers all HTTP routes. Also installs a shutdown hook that closes
 * the database pool when the server stops.
 */
fun Application.module() {
    // ----- Plugins -----
    install(ContentNegotiation) {
        json()
    }
    install(CallLogging)
    install(CORS) {
        anyHost() // Restrict in production. For dev/TFG, allow all origins.
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
    }

    // ----- Database -----
    DatabaseFactory.init(environment.config)

    // ----- Shutdown hook -----
    monitor.subscribe(ApplicationStopped) {
        DatabaseFactory.close()
    }

    // ----- Routing -----
    routing {
        healthRoutes()
    }
}