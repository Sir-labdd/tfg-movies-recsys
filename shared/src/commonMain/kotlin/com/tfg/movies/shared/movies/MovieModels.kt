package com.tfg.movies.shared.movies

import kotlinx.serialization.Serializable

/**
 * DTOs (Data Transfer Objects) shared between the backend server and
 * any client (e.g. Compose Multiplatform frontend).
 *
 * Living in :shared/commonMain means they compile for every target
 * declared in the module (currently JVM and JS (browser). The JVM target is consumed by
 * :server, the JS target by :composeApp).
 *
 * No JVM-specific imports (java.sql, java.time, etc.) are allowed in
 * this file — that constraint is what keeps the module truly shareable.
 */

/**
 * Detailed view of a movie with all related entities aggregated.
 */
@Serializable
data class MovieDetails(
    val id: Int,
    val title: String,
    val originalTitle: String?,
    val originalLanguage: String?,
    val overview: String?,
    val tagline: String?,
    val releaseDate: String?, // ISO format YYYY-MM-DD
    val runtime: Int?,
    val status: String?,
    val budget: Long?,
    val revenue: Long?,
    val popularity: Double?,
    val voteAverage: Double?,
    val voteCount: Int?,
    val adult: Boolean,
    val posterPath: String?,
    val imdbId: String?,
    val collection: CollectionRef?,
    val genres: List<String>,
    val productionCountries: List<String>,
    val spokenLanguages: List<String>,
    val productionCompanies: List<String>,
    val keywords: List<String>,
    val cast: List<CastMember>,
    val crew: List<CrewMember>,
    val crossReferences: CrossReferences,
)

@Serializable
data class CollectionRef(
    val id: Int,
    val name: String,
    val posterPath: String?,
)

@Serializable
data class CastMember(
    val personId: Int,
    val name: String,
    val character: String?,
    val order: Int,
)

@Serializable
data class CrewMember(
    val personId: Int,
    val name: String,
    val job: String?,
    val department: String?,
)

@Serializable
data class CrossReferences(
    val movielensId: Int?,
    val imdbNumeric: String?,
)
