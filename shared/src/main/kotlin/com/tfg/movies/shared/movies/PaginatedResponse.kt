package com.tfg.movies.shared.pagination

import kotlinx.serialization.Serializable

/**
 * Generic paginated response wrapper used by any list endpoint.
 *
 * Lives in its own subpackage so other features (people, genres,
 * collections) can reuse it without depending on the movies module.
 *
 * - items: the slice of results for the current page
 * - page: 1-based current page number
 * - pageSize: requested page size (may be smaller for the last page)
 * - total: total number of items across all pages
 * - totalPages: convenience field equal to ceil(total / pageSize)
 *
 * Generic type T is constrained to be a class with kotlinx.serialization
 * via the @Serializable annotation on T at use site.
 */
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val total: Long,
    val totalPages: Long,
)