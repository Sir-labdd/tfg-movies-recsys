package com.tfg.movies.shared.movies

import kotlinx.serialization.Serializable

/**
 * Lightweight representation of a movie for listings.
 *
 * Used by GET /movies (and any future endpoint returning a list of
 * movies, e.g. /movies/search or /movies/{id}/similar). Contains only
 * the fields a card-like UI needs, keeping payloads small even for
 * large pages.
 *
 * For the full detail of a single movie, use MovieDetails instead.
 */
@Serializable
data class MovieSummary(
    val id: Int,
    val title: String,
    val year: Int?, // Extracted from release_date. Null if the movie has no date.
    val posterPath: String?,
    val voteAverage: Double?,
    val voteCount: Int?,
    val popularity: Double?,
    val genres: List<String>,
)