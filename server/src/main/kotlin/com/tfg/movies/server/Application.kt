package com.tfg.movies.server

import com.tfg.movies.server.config.DatabaseFactory
import com.tfg.movies.server.health.healthRoutes
import com.tfg.movies.server.movies.MovieRepository
import com.tfg.movies.server.movies.MovieService
import com.tfg.movies.server.movies.movieRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.staticResources

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    // ----- Plugins -----
    install(ContentNegotiation) { json() }
    install(CallLogging)
    install(CORS) {
        anyHost()
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

    // ----- Manual dependency wiring -----
    val movieRepository = MovieRepository()
    val movieService = MovieService(movieRepository)

    // ----- Routing -----
    routing {
        healthRoutes()
        movieRoutes(movieService)

        // Serve the production frontend bundle. Files are copied from
        // :composeApp's webpack output into the "static" classpath
        // directory by the copyFrontend Gradle task (see server/
        // build.gradle.kts). Placed AFTER the API routes so they
        // take priority over static file resolution.
        staticResources("/", "static")
    }
}