package com.tfg.movies.shared.errors

import kotlinx.serialization.Serializable

/**
 * Standard error response body returned by every API endpoint on
 * failure. Frontend code relies on this shape to render messages
 * uniformly regardless of which endpoint failed.
 *
 * - `error`: stable machine-readable code (e.g. "movie_not_found").
 *   Frontends may switch on it to render different UI states.
 * - `message`: human-readable description, suitable for logs or
 *   developer-facing displays. Not intended for end users directly.
 *
 * Lives in :shared/errors (not under any feature subpackage) because
 * it is the common error contract for all endpoints.
 */
@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
)