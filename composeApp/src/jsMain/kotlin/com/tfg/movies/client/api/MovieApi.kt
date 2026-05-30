package com.tfg.movies.client.api

import com.tfg.movies.shared.movies.MovieDetails
import com.tfg.movies.shared.movies.MovieSortBy
import com.tfg.movies.shared.movies.MovieSummary
import com.tfg.movies.shared.movies.SimilarMoviesResponse
import com.tfg.movies.shared.movies.SortDirection
import com.tfg.movies.shared.pagination.PaginatedResponse

/**
 * Filters for the GET /movies listing endpoint, grouped into a single
 * data class so callers don't have to pass eight nullable parameters.
 *
 * Lives in :composeApp (not :shared) because the grouping is a client-
 * side convenience, not part of the HTTP contract. The backend receives
 * the parameters individually as query string entries.
 */
data class MovieListFilters(
    val genre: String? = null,
    val year: Int? = null,
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val language: String? = null,
    val minVoteCount: Int? = null,
    val sortBy: MovieSortBy = MovieSortBy.POPULARITY,
    val sortDir: SortDirection = SortDirection.DESC,
)

/**
 * High-level API client for the movies backend.
 *
 * Each public method maps to one backend endpoint, constructs the URL
 * with the proper query string via ApiClient.buildParams, delegates the
 * actual HTTP call to ApiClient.get<T>, and returns Result<T>. Errors
 * are exposed as ApiException inside Result.failure; callers fold on
 * Result to handle success and failure paths in a unified way.
 *
 *     val result: Result<MovieDetails> = MovieApi.getMovie(550)
 *     result.fold(
 *         onSuccess = { movie -> /* display the movie */ },
 *         onFailure = { error -> /* show error.userMessage */ }
 *     )
 *
 * This object holds no state of its own; all the configuration (base
 * URL, HTTP client, serialization) is owned by ApiClient and ApiConfig.
 */
object MovieApi {

    /**
     * GET /movies — paginated, filtered, sorted listing.
     */
    suspend fun getMovies(
        filters: MovieListFilters = MovieListFilters(),
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<PaginatedResponse<MovieSummary>> {
        val params = ApiClient.buildParams(
            "genre" to filters.genre,
            "year" to filters.year,
            "yearFrom" to filters.yearFrom,
            "yearTo" to filters.yearTo,
            "language" to filters.language,
            "minVoteCount" to filters.minVoteCount,
            "sortBy" to filters.sortBy.toApiParam(),
            "sortDir" to filters.sortDir.toApiParam(),
            "page" to page,
            "pageSize" to pageSize,
        )
        return ApiClient.get("/movies$params")
    }

    /**
     * GET /movies/{id} — full details for one movie.
     */
    suspend fun getMovie(id: Int): Result<MovieDetails> {
        return ApiClient.get("/movies/$id")
    }

    /**
     * GET /movies/search?q=... — search by title text.
     */
    suspend fun searchMovies(
        query: String,
        page: Int = 1,
        pageSize: Int = 20,
    ): Result<PaginatedResponse<MovieSummary>> {
        val params = ApiClient.buildParams(
            "q" to query,
            "page" to page,
            "pageSize" to pageSize,
        )
        return ApiClient.get("/movies/search$params")
    }

    /**
     * GET /movies/{id}/similar — semantic recommendations.
     */
    suspend fun getSimilarMovies(
        id: Int,
        limit: Int = 10,
        minVoteCount: Int? = null,
    ): Result<SimilarMoviesResponse> {
        val params = ApiClient.buildParams(
            "limit" to limit,
            "minVoteCount" to minVoteCount,
        )
        return ApiClient.get("/movies/$id/similar$params")
    }

    /**
     * GET /genres — list of all genre names for filter dropdowns.
     */
    suspend fun getGenres(): Result<List<String>> {
        return ApiClient.get("/genres")
    }
}

/**
 * Convert sort-by enum to the camelCase string the backend expects.
 * Mirrors the @SerialName values declared in MovieSortBy, but done
 * explicitly here because we are building a URL query string, not
 * serializing JSON (where kotlinx-serialization would do this for us).
 */
private fun MovieSortBy.toApiParam(): String = when (this) {
    MovieSortBy.POPULARITY   -> "popularity"
    MovieSortBy.VOTE_AVERAGE -> "voteAverage"
    MovieSortBy.VOTE_COUNT   -> "voteCount"
    MovieSortBy.RELEASE_DATE -> "releaseDate"
    MovieSortBy.TITLE        -> "title"
}

private fun SortDirection.toApiParam(): String = when (this) {
    SortDirection.ASC  -> "asc"
    SortDirection.DESC -> "desc"
}