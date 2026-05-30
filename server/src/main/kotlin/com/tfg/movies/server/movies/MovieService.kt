package com.tfg.movies.server.movies

import com.tfg.movies.server.movies.MovieRepository.ListFilters
import com.tfg.movies.server.movies.MovieRepository.PagedResult
import com.tfg.movies.server.movies.MovieRepository.Pagination
import com.tfg.movies.server.movies.MovieRepository.SortSpec
import com.tfg.movies.shared.movies.MovieDetails
import com.tfg.movies.shared.movies.MovieSortBy
import com.tfg.movies.shared.movies.SortDirection

/**
 * Business logic layer for movies.
 *
 * Validates input received from the routing layer, applies sensible
 * defaults, and delegates to the repository. Routes never call the
 * repository directly: all access goes through the service.
 *
 * Validation failures are signaled by throwing [ValidationException].
 * The routing layer catches it and maps it to a 400 response.
 */
class MovieService(
    private val repository: MovieRepository,
) {

    fun getById(id: Int): MovieDetails? = repository.findById(id)

    /**
     * Returns a page of movies matching the given filters.
     *
     * @throws ValidationException if any input is out of acceptable bounds.
     */
    fun list(
        filters: ListFilters,
        sort: SortSpec,
        page: Int,
        pageSize: Int,
    ): PagedResult {
        validatePagination(page, pageSize)
        validateYearFilters(filters)

        val effectiveFilters = applyAutoMinVoteCount(filters, sort)
        val pagination = Pagination(page = page, pageSize = pageSize)

        return repository.findAll(effectiveFilters, sort, pagination)
    }

    /**
     * Searches movies by title. Validates the query string and
     * delegates to the repository.
     *
     * @throws ValidationException if the query is too short, too long,
     *         or the pagination parameters are out of bounds.
     */
    fun search(
        query: String,
        page: Int,
        pageSize: Int,
    ): MovieRepository.PagedResult {
        validateSearchQuery(query)
        validatePagination(page, pageSize)

        val pagination = MovieRepository.Pagination(page = page, pageSize = pageSize)
        return repository.searchByTitle(query.trim(), pagination)
    }

    private fun validateSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < MIN_SEARCH_QUERY_LENGTH) {
            throw ValidationException(
                code = "invalid_search_query",
                message = "Search query must be at least $MIN_SEARCH_QUERY_LENGTH characters",
            )
        }
        if (trimmed.length > MAX_SEARCH_QUERY_LENGTH) {
            throw ValidationException(
                code = "invalid_search_query",
                message = "Search query must be at most $MAX_SEARCH_QUERY_LENGTH characters",
            )
        }
    }

    /**
     * Returns the list of movies most similar to the reference movie.
     *
     * @throws ValidationException if limit is out of bounds.
     * @return null if the reference movie has no embedding (route maps to 404).
     */
    fun findSimilar(
        referenceId: Int,
        limit: Int,
        minVoteCount: Int? = null,
    ): List<Pair<com.tfg.movies.shared.movies.MovieSummary, Double>>? {
        validateSimilarLimit(limit)
        if (minVoteCount != null && minVoteCount < 0) {
            throw ValidationException(
                code = "invalid_min_vote_count",
                message = "minVoteCount cannot be negative, got: $minVoteCount",
            )
        }
        return repository.findSimilarTo(referenceId, limit, minVoteCount)
    }

    private fun validateSimilarLimit(limit: Int) {
        if (limit < 1 || limit > MAX_SIMILAR_LIMIT) {
            throw ValidationException(
                code = "invalid_limit",
                message = "limit must be between 1 and $MAX_SIMILAR_LIMIT, got: $limit",
            )
        }
    }

    /**
     * Returns all available genre names for filter dropdowns.
     */
    fun getAllGenres(): List<String> = repository.findAllGenreNames()

    // ---------- validations ----------

    private fun validatePagination(page: Int, pageSize: Int) {
        if (page < 1) {
            throw ValidationException(
                code = "invalid_page",
                message = "Page must be >= 1, got: $page",
            )
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw ValidationException(
                code = "invalid_page_size",
                message = "pageSize must be between 1 and $MAX_PAGE_SIZE, got: $pageSize",
            )
        }
    }

    private fun validateYearFilters(filters: ListFilters) {
        if (filters.yearFrom != null && filters.yearTo != null &&
            filters.yearFrom > filters.yearTo
        ) {
            throw ValidationException(
                code = "invalid_year_range",
                message = "yearFrom (${filters.yearFrom}) cannot be greater than yearTo (${filters.yearTo})",
            )
        }
        if (filters.minVoteCount != null && filters.minVoteCount < 0) {
            throw ValidationException(
                code = "invalid_min_vote_count",
                message = "minVoteCount cannot be negative, got: ${filters.minVoteCount}",
            )
        }
    }

    // ---------- smart defaults ----------

    /**
     * When sorting by voteAverage without an explicit minVoteCount,
     * apply a sensible default. Otherwise the top of the list fills
     * with obscure movies that have a single 10/10 vote.
     *
     * Documented in the API as a behavior, not as silent magic: if
     * the caller wants to include low-vote movies, they pass
     * minVoteCount=0 explicitly.
     */
    private fun applyAutoMinVoteCount(
        filters: ListFilters,
        sort: SortSpec,
    ): ListFilters {
        if (sort.by == MovieSortBy.VOTE_AVERAGE && filters.minVoteCount == null) {
            return filters.copy(minVoteCount = DEFAULT_MIN_VOTE_COUNT_FOR_RATING_SORT)
        }
        return filters
    }

    companion object {
        const val MAX_PAGE_SIZE = 100

        /**
         * Minimum number of votes required when sorting by voteAverage
         * if the caller did not specify their own minVoteCount. This
         * filters out statistical noise (movies with very few votes).
         */
        const val DEFAULT_MIN_VOTE_COUNT_FOR_RATING_SORT = 10
        const val MIN_SEARCH_QUERY_LENGTH = 2
        const val MAX_SEARCH_QUERY_LENGTH = 100

        const val MAX_SIMILAR_LIMIT = 50
        const val DEFAULT_SIMILAR_LIMIT = 10

    }
}

/**
 * Thrown when the service receives input that violates a business
 * rule. Carries a machine-readable code so the routing layer can
 * build a stable ErrorResponse payload.
 */
class ValidationException(
    val code: String,
    message: String,
) : RuntimeException(message)