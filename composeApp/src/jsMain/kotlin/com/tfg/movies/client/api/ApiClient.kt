package com.tfg.movies.client.api

import com.tfg.movies.shared.errors.ErrorResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Native JavaScript function for percent-encoding URI components.
 * Declared as `external` so Kotlin can call it without a wrapper.
 */
external fun encodeURIComponent(value: String): String

/**
 * Generic HTTP client for the backend API.
 *
 * Wraps a Ktor HttpClient configured with kotlinx.serialization JSON
 * and exposes two reusable building blocks for the higher-level
 * MovieApi:
 *
 *   - `get<T>(path)`: performs a GET request and deserializes the
 *     response body into T, returning Result<T>. Non-2xx responses
 *     are mapped to ApiException with the server's machine-readable
 *     error code when available. Transport failures (network errors)
 *     are mapped to ApiException with status=0 and a generic message.
 *
 *   - `buildParams(...)`: builds a query string from name/value pairs,
 *     dropping pairs whose value is null. Both keys and values are
 *     percent-encoded.
 */
object ApiClient {

    @PublishedApi
    internal val httpClient = HttpClient(Js) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * Perform a GET request and deserialize the response body to T.
     * Returns `Result.success(body)` on 2xx, `Result.failure(ApiException)`
     * otherwise.
     */
    suspend inline fun <reified T> get(path: String): Result<T> = runCatching {
        val response: HttpResponse = httpClient.get("${ApiConfig.baseUrl}$path")
        if (response.status.value !in 200..299) {
            throw mapErrorResponse(response)
        }
        response.body<T>()
    }.recoverCatching { throwable ->
        when (throwable) {
            is ApiException -> throw throwable
            else -> throw ApiException(
                status = 0,
                errorCode = ApiException.NETWORK_ERROR,
                userMessage = "Could not reach the server: ${throwable.message ?: "unknown error"}",
            )
        }
    }

    /**
     * Build a query string from the given (name, value) pairs.
     *
     * Pairs whose value is null are dropped, so the caller can pass
     * every possible parameter unconditionally and let buildParams
     * figure out which ones to include.
     *
     * Example:
     *     buildParams("genre" to "Drama", "page" to 1, "year" to null)
     *     // -> "?genre=Drama&page=1"
     */
    fun buildParams(vararg pairs: Pair<String, Any?>): String {
        val nonNullPairs = pairs.filter { it.second != null }
        if (nonNullPairs.isEmpty()) return ""
        return nonNullPairs.joinToString(prefix = "?", separator = "&") { (key, value) ->
            "${encodeURIComponent(key)}=${encodeURIComponent(value.toString())}"
        }
    }

    /**
     * Try to read the response body as ErrorResponse (the server's
     * structured error contract); fall back to a generic message
     * when the body is not parseable.
     */

    @PublishedApi
    internal suspend fun mapErrorResponse(response: HttpResponse): ApiException {
        return try {
            val body = response.body<ErrorResponse>()
            ApiException(
                status = response.status.value,
                errorCode = body.error,
                userMessage = body.message,
            )
        } catch (e: Throwable) {
            ApiException(
                status = response.status.value,
                errorCode = ApiException.PARSE_ERROR,
                userMessage = "Server returned ${response.status.value} but its body could not be parsed.",
            )
        }
    }
}