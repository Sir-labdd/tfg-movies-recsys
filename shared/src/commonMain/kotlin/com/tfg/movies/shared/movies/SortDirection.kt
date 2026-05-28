package com.tfg.movies.shared.movies

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sort direction for any listing.
 */
@Serializable
enum class SortDirection {
    @SerialName("asc")
    ASC,

    @SerialName("desc")
    DESC,
}