package com.tfg.movies.shared.movies

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Fields by which movie listings can be sorted.
 *
 * The @SerialName annotation maps each enum constant to the query
 * string value that clients use (camelCase), keeping the Kotlin
 * convention of UPPER_SNAKE_CASE for enum constants.
 */
@Serializable
enum class MovieSortBy {
    @SerialName("popularity")
    POPULARITY,

    @SerialName("voteAverage")
    VOTE_AVERAGE,

    @SerialName("voteCount")
    VOTE_COUNT,

    @SerialName("releaseDate")
    RELEASE_DATE,

    @SerialName("title")
    TITLE,
}