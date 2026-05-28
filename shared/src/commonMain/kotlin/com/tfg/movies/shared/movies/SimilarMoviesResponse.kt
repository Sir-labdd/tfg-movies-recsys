package com.tfg.movies.shared.movies

import kotlinx.serialization.Serializable

/**
 * Response wrapper for GET /movies/{id}/similar.
 *
 * Includes movieId at the top level so the consumer can verify which
 * movie the recommendations correspond to, even if the response is
 * processed out of context.
 */
@Serializable
data class SimilarMoviesResponse(
    val movieId: Int,
    val items: List<SimilarMovieItem>,
)