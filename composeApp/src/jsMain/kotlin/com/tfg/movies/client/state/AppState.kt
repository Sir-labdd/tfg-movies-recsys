package com.tfg.movies.client.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.window

/**
 * Application-wide navigation state.
 *
 * Maintains a single observable [currentRoute] string that determines
 * which screen is rendered. Integrates with the browser's History API
 * so that:
 *   - navigateTo() pushes a new entry, updating the URL bar.
 *   - The browser's back/forward buttons fire popstate events, which
 *     this object listens for and translates into route changes.
 *   - Direct URL access (e.g. bookmarking /movie/550) is handled by
 *     reading window.location on initialization.
 *
 * Route format:
 *   "movies"       → listing screen (default)
 *   "movie/{id}"   → detail screen for movie with that id
 *   "search/{q}"   → search results (added in B7.6)
 */
object AppState {

    var currentRoute: String by mutableStateOf(initialRoute())

    fun navigateTo(route: String) {
        currentRoute = route
        window.history.pushState(null, "", "/#/$route")
    }

    fun goBack() {
        window.history.back()
    }

    /**
     * Call once from Main.kt to start listening for browser
     * back/forward navigation.
     */
    fun initPopStateListener() {
        window.onpopstate = {
            currentRoute = routeFromHash()
        }
    }

    private fun initialRoute(): String = routeFromHash()

    /**
     * Parse the route from the URL hash fragment.
     *
     * We use hash-based routing (/#/movie/550) rather than
     * path-based (/movie/550) because the Webpack dev server and
     * the future Ktor static-file server both need to serve
     * index.html for every path. Hash routing avoids that
     * complexity entirely: the server always sees "/" regardless
     * of the route, and the client reads the fragment.
     */
    private fun routeFromHash(): String {
        val hash = window.location.hash // e.g. "#/movie/550"
        return if (hash.startsWith("#/")) {
            hash.removePrefix("#/")
        } else {
            "movies" // default route
        }
    }
}