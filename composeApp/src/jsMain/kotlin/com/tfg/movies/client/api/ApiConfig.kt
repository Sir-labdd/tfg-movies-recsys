package com.tfg.movies.client.api

import kotlinx.browser.window

/**
 * Centralizes API configuration.
 *
 * Resolution order for the backend base URL:
 *   1. window.API_BASE_URL — explicit override injected in the host
 *      HTML (used in custom deployments).
 *   2. Auto-detection based on the current origin:
 *      - If running on port 8081 (webpack-dev-server), the API is
 *        assumed to be on localhost:8080 (two-process development).
 *      - Otherwise, empty string: the frontend and API share the
 *        same origin (single-process production via B7.8), so
 *        fetch() uses relative paths ("/movies" instead of
 *        "http://localhost:8080/movies").
 */
object ApiConfig {

    val baseUrl: String
        get() {
            // 1. Explicit override.
            val override = window.asDynamic().API_BASE_URL
            if (override != null && override.toString().isNotBlank()) {
                return override.toString()
            }

            // 2. Auto-detect.
            val port = window.location.port
            return if (port == "8081") {
                // Webpack dev server → backend on 8080.
                "http://localhost:8080"
            } else {
                // Same origin (Ktor serves both API and frontend).
                ""
            }
        }
}