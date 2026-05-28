package com.tfg.movies.client.api

import kotlinx.browser.window

/**
 * Centralizes API configuration. Currently only the base URL of the
 * backend.
 *
 * Resolution order:
 *   1. window.API_BASE_URL — a JavaScript global that can be injected
 *      by the host HTML (typically in production, where the deployed
 *      bundle is the same but the backend URL differs).
 *   2. "http://localhost:8080" — fallback for development.
 *
 * The host HTML can opt-in to the override by including a small
 * inline script BEFORE composeApp.js loads:
 *
 *     <script>
 *         window.API_BASE_URL = "https://api.production.example.com";
 *     </script>
 *     <script src="composeApp.js"></script>
 *
 * Leaving the override commented out is normal in development. The
 * JavaScript engine will simply see `window.API_BASE_URL === undefined`
 * and the fallback applies.
 */
object ApiConfig {

    val baseUrl: String
        get() {
            val override = window.asDynamic().API_BASE_URL
            return if (override != null && override.toString().isNotBlank()) {
                override.toString()
            } else {
                "http://localhost:8080"
            }
        }
}
