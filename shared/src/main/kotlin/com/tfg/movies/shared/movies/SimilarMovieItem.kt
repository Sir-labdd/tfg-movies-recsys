package com.tfg.movies.shared.movies

import kotlinx.serialization.Serializable

/**
 * One item in a list of similar-movie recommendations.
 *
 * Wraps the standard MovieSummary with the cosine similarity score
 * (between 0 and 1) computed against the reference movie.
 *
 * Higher scores indicate stronger semantic similarity. Frontend may
 * choose to display the score (debug/explainability) or hide it
 * (clean UX). Either way the API exposes it so the choice belongs to
 * the consumer.
 */
@Serializable
data class SimilarMovieItem(
    val movie: MovieSummary,
    val similarity: Double,
)