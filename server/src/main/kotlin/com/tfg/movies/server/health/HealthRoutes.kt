package com.tfg.movies.server.health

import com.tfg.movies.server.config.DatabaseFactory
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

/**
 * Health check endpoint exposed at GET /health.
 *
 * Returns 200 OK with a JSON body indicating the service status and
 * whether the database is reachable. Returns 503 if the database
 * cannot be reached, even if the server itself is responsive — this
 * is the correct behavior for service health checks consumed by load
 * balancers or monitoring systems.
 */

/**
 * Response payload for the /health endpoint.
 */
@Serializable
data class HealthResponse(
    val status: String,
    val database: String,
)

fun Route.healthRoutes() {
    get("/health") {
        val dbHealthy = DatabaseFactory.isHealthy()
        val response = HealthResponse(
            status = if (dbHealthy) "ok" else "degraded",
            database = if (dbHealthy) "up" else "down",
        )
        val statusCode = if (dbHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        call.respond(statusCode, response)
    }
}