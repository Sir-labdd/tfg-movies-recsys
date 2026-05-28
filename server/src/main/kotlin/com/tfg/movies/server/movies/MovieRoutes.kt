package com.tfg.movies.server.movies

import com.tfg.movies.server.movies.MovieRepository.ListFilters
import com.tfg.movies.server.movies.MovieRepository.SortSpec
import com.tfg.movies.shared.errors.ErrorResponse
import com.tfg.movies.shared.movies.MovieSortBy
import com.tfg.movies.shared.movies.MovieSummary
import com.tfg.movies.shared.movies.SortDirection
import com.tfg.movies.shared.pagination.PaginatedResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.math.ceil
import com.tfg.movies.shared.movies.SimilarMovieItem
import com.tfg.movies.shared.movies.SimilarMoviesResponse
/**
 * HTTP routes for movies.
 *
 * Responsibilities:
 *   - Parse path and query parameters into typed values.
 *   - Reject malformed parameters with 400 before reaching the service.
 *   - Catch validation exceptions thrown by the service and translate
 *     them into 400 responses with a structured ErrorResponse.
 *   - Build the PaginatedResponse wrapper for list endpoints.
 */
fun Route.movieRoutes(service: MovieService) {

    route("/movies") {

        // GET /movies — paginated list with optional filters and sort
        get {
            try {
                val page = call.parseIntQuery("page", default = 1)
                val pageSize = call.parseIntQuery("pageSize", default = 20)

                val sortBy = call.parseEnumQuery<MovieSortBy>(
                    name = "sortBy",
                    default = MovieSortBy.POPULARITY,
                    fromString = { value ->
                        MovieSortBy.entries.firstOrNull { it.serialName == value }
                    },
                )
                val sortDir = call.parseEnumQuery<SortDirection>(
                    name = "sortDir",
                    default = SortDirection.DESC,
                    fromString = { value ->
                        SortDirection.entries.firstOrNull { it.serialName == value }
                    },
                )

                val filters = ListFilters(
                    genres = call.parameters.getAll("genre") ?: emptyList(),
                    year = call.parseIntQueryOrNull("year"),            // ahora ...OrNull
                    yearFrom = call.parseIntQueryOrNull("yearFrom"),    // ahora ...OrNull
                    yearTo = call.parseIntQueryOrNull("yearTo"),        // ahora ...OrNull
                    language = call.parameters["language"],
                    minVoteCount = call.parseIntQueryOrNull("minVoteCount"), // ahora ...OrNull
                )

                val result = service.list(
                    filters = filters,
                    sort = SortSpec(by = sortBy, direction = sortDir),
                    page = page,
                    pageSize = pageSize,
                )

                val totalPages = if (result.total == 0L) 0L
                else ceil(result.total.toDouble() / pageSize).toLong()

                val response = PaginatedResponse(
                    items = result.items,
                    page = page,
                    pageSize = pageSize,
                    total = result.total,
                    totalPages = totalPages,
                )
                call.respond(HttpStatusCode.OK, response)

            } catch (e: BadQueryParamException) {
                call.respond(HttpStatusCode.BadRequest, e.toResponse())
            } catch (e: ValidationException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(error = e.code, message = e.message ?: "validation failed"),
                )
            }
        }

        // GET /movies/search?q=...
        get("/search") {
            try {
                val q = call.parameters["q"]
                if (q.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            error = "missing_query",
                            message = "Query parameter 'q' is required",
                        ),
                    )
                    return@get
                }

                val page = call.parseIntQuery("page", default = 1)
                val pageSize = call.parseIntQuery("pageSize", default = 20)

                val result = service.search(
                    query = q,
                    page = page,
                    pageSize = pageSize,
                )

                val totalPages = if (result.total == 0L) 0L
                else ceil(result.total.toDouble() / pageSize).toLong()

                val response = PaginatedResponse(
                    items = result.items,
                    page = page,
                    pageSize = pageSize,
                    total = result.total,
                    totalPages = totalPages,
                )
                call.respond(HttpStatusCode.OK, response)

            } catch (e: BadQueryParamException) {
                call.respond(HttpStatusCode.BadRequest, e.toResponse())
            } catch (e: ValidationException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(error = e.code, message = e.message ?: "validation failed"),
                )
            }
        }

        // GET /movies/{id}/similar
        get("/{id}/similar") {
            try {
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

                val limit = call.parseIntQuery("limit", default = MovieService.DEFAULT_SIMILAR_LIMIT)
                val minVoteCount = call.parseIntQueryOrNull("minVoteCount")

                val results = service.findSimilar(id, limit, minVoteCount)
                if (results == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse(
                            error = "movie_not_found",
                            message = "No movie with id $id has an embedding " +
                                    "(either it does not exist or its overview is too short).",
                        ),
                    )
                    return@get
                }

                val items = results.map { (summary, similarity) ->
                    SimilarMovieItem(movie = summary, similarity = similarity)
                }
                call.respond(
                    HttpStatusCode.OK,
                    SimilarMoviesResponse(movieId = id, items = items),
                )

            } catch (e: BadQueryParamException) {
                call.respond(HttpStatusCode.BadRequest, e.toResponse())
            } catch (e: ValidationException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(error = e.code, message = e.message ?: "validation failed"),
                )
            }
        }

        // GET /movies/{id} — full detail of one movie
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

// ---------- query param parsing helpers ----------


/**
 * Reads a required integer query parameter that has a non-null default.
 * Returns either the parsed value or the default.
 * Throws [BadQueryParamException] if present but not a valid integer.
 */
private fun ApplicationCall.parseIntQuery(name: String, default: Int): Int {
    val raw = parameters[name] ?: return default
    return raw.toIntOrNull() ?: throw BadQueryParamException(
        param = name,
        value = raw,
        expected = "integer",
    )
}

/**
 * Reads an optional integer query parameter. Returns null if absent.
 * Throws [BadQueryParamException] if present but not a valid integer.
 */
private fun ApplicationCall.parseIntQueryOrNull(name: String): Int? {
    val raw = parameters[name] ?: return null
    return raw.toIntOrNull() ?: throw BadQueryParamException(
        param = name,
        value = raw,
        expected = "integer",
    )
}
/**
 * Reads an enum-typed query parameter using the serialName mapping.
 * The [fromString] lambda owns the conversion so each enum can use
 * its own serialName scheme.
 */
private inline fun <reified E : Enum<E>> ApplicationCall.parseEnumQuery(
    name: String,
    default: E,
    fromString: (String) -> E?,
): E {
    val raw = parameters[name] ?: return default
    return fromString(raw) ?: throw BadQueryParamException(
        param = name,
        value = raw,
        expected = enumValues<E>().joinToString(", ") { it.serialName },
    )
}

/**
 * Best-effort retrieval of an enum's @SerialName via reflection-free
 * lookup. kotlinx.serialization stores serialNames in the enum's
 * descriptor at compile time, but here we just want the string form
 * used at runtime. Adding a name() convention keeps the helpers
 * decoupled from the serialization runtime.
 *
 * Each enum exposes a 'serialName' through a custom extension below.
 */
private val Enum<*>.serialName: String
    get() = when (this) {
        is MovieSortBy -> when (this) {
            MovieSortBy.POPULARITY -> "popularity"
            MovieSortBy.VOTE_AVERAGE -> "voteAverage"
            MovieSortBy.VOTE_COUNT -> "voteCount"
            MovieSortBy.RELEASE_DATE -> "releaseDate"
            MovieSortBy.TITLE -> "title"
        }
        is SortDirection -> when (this) {
            SortDirection.ASC -> "asc"
            SortDirection.DESC -> "desc"
        }
        else -> name
    }

/**
 * Thrown when a query parameter is present but malformed. Carries
 * enough context for the response to tell the caller what was wrong.
 */
private class BadQueryParamException(
    val param: String,
    val value: String,
    val expected: String,
) : RuntimeException("Bad value for '$param': '$value' (expected $expected)") {
    fun toResponse(): ErrorResponse = ErrorResponse(
        error = "invalid_query_param",
        message = message ?: "invalid query parameter",
    )
}