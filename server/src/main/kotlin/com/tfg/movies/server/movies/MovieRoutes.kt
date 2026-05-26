package com.tfg.movies.server.movies

import com.tfg.movies.shared.errors.ErrorResponse
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * HTTP routes for movies. Parses path parameters, delegates to the
 * service, and translates the result into the correct HTTP response.
 */
fun Route.movieRoutes(service: MovieService) {

    route("/movies") {

        // GET /movies/{id}
        get("/{id}") {
            val idParam = call.parameters["id"]
            val id = idParam?.toIntOrNull()

            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        error = "invalid_id",
                        message = "Path parameter 'id' must be an integer, got: $idParam",
                    ),
                )
                return@get
            }

            val movie = service.getById(id)
            if (movie == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        error = "movie_not_found",
                        message = "No movie exists with id $id",
                    ),
                )
                return@get
            }

            call.respond(HttpStatusCode.OK, movie)
        }
    }
}