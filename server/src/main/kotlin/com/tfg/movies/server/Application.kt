package com.tfg.movies.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
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
 * Application module. Called automatically by Ktor via the
 * application.yaml configuration (modules: [ApplicationKt.module]).
 *
 * Wires together routing, content negotiation, error pages, etc.
 * Right now it only exposes /health for connectivity checks.
 */
fun Application.module() {
    routing {
        get("/health") {
            call.respondText("OK")
        }
    }
}