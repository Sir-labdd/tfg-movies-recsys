package com.tfg.movies.server

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json

/**
 * Test helper: builds a Ktor test application that mirrors the
 * production wiring defined in [com.tfg.movies.server.module],
 * connected to the same PostgreSQL instance used in development.
 *
 * Why share the development database:
 *  - All endpoints under test in B5 are read-only.
 *  - The database is already loaded with the full 45,408-movie
 *    dataset, so tests can assert on real, stable data.
 *  - Testcontainers is the planned alternative for B7 once
 *    write endpoints are introduced and isolation matters.
 *
 * Each call to [runWithApp] starts a fresh testApplication instance,
 * runs the supplied block (which receives a pre-configured HttpClient),
 * and shuts the app down cleanly.
 */
fun runWithApp(block: suspend (HttpClient) -> Unit) = testApplication {

    // Provide the application config inline. This replaces the YAML
    // file used in production, so the test does not depend on
    // environment variables being set at JVM startup.
    environment {
        config = MapApplicationConfig(
            "ktor.application.modules.size" to "1",
            "ktor.application.modules.0" to "com.tfg.movies.server.ApplicationKt.module",
            "database.jdbcUrl" to (System.getenv("DATABASE_JDBC_URL")
                ?: "jdbc:postgresql://localhost:5432/tfg_movies"),
            "database.user" to (System.getenv("DATABASE_USER") ?: "tfg_user"),
            "database.password" to (System.getenv("DATABASE_PASSWORD") ?: "tfg_local_dev"),
            "database.poolSize" to "5",
        )
    }

    // Configure the HttpClient that the test will use to call the
    // server. Installing ContentNegotiation lets us call
    // response.body<T>() and have the JSON parsed automatically.
    val client = createClient {
        install(ClientContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    block(client)
}