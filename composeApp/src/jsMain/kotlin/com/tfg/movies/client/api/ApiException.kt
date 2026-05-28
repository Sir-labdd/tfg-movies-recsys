package com.tfg.movies.client.api

/**
 * Exception thrown by [ApiClient] when a request to the backend fails.
 *
 * @param status HTTP status code received from the server (or 0 if the
 *               request could not even reach the server — network error,
 *               CORS rejection, etc.).
 * @param errorCode Machine-readable code identifying the error class.
 *                  When the server returned a structured ErrorResponse
 *                  body, this is the `error` field (e.g. "movie_not_found",
 *                  "invalid_id"). For transport-level failures it is one
 *                  of the constants declared below.
 * @param userMessage Human-readable message safe to display in the UI.
 *                    Derived from the server's ErrorResponse.message when
 *                    available, otherwise a generic fallback per category.
 */
class ApiException(
    val status: Int,
    val errorCode: String,
    val userMessage: String,
) : Exception("[$status $errorCode] $userMessage") {

    companion object {
        // Transport-level error codes (no HTTP status from server).
        const val NETWORK_ERROR = "network_error"
        const val PARSE_ERROR = "parse_error"
        const val UNKNOWN_ERROR = "unknown_error"
    }
}
