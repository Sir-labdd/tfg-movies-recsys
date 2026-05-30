package com.tfg.movies.client.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tfg.movies.client.api.ApiException
import com.tfg.movies.client.api.MovieApi
import com.tfg.movies.client.api.MovieListFilters
import com.tfg.movies.client.components.MovieFilters
import com.tfg.movies.client.components.MovieGrid
import com.tfg.movies.shared.movies.MovieSummary
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import com.tfg.movies.client.state.AppState
/**
 * Main listing screen with filters and infinite scroll.
 *
 * State model:
 *   - movies: accumulated list across all loaded pages.
 *   - currentPage: last successfully loaded page number.
 *   - hasMore: false when the last page returned fewer items than
 *     pageSize, meaning we've reached the end of the catalog.
 *   - isLoadingMore: true while a scroll-triggered page load is
 *     in flight. Prevents duplicate fetches.
 *   - initialLoading: true during the very first fetch or after a
 *     filter change resets the list.
 *   - error: non-null when the most recent fetch failed.
 *   - filters: current filter state (genre, sort, direction).
 *   - genres: list of genre names loaded once from /genres.
 *
 * Two fetch mechanisms:
 *   1. LaunchedEffect(filterKey): fires on mount and whenever
 *      filters change. Resets movies to empty, sets page=1, and
 *      fetches the first page.
 *   2. Scroll listener (DisposableEffect): detects proximity to
 *      the bottom of the viewport and triggers loading of the
 *      next page via scope.launch.
 */
@Composable
fun MovieListScreen() {
    val scope = rememberCoroutineScope()

    // ---- State ----
    var movies by remember { mutableStateOf<List<MovieSummary>>(emptyList()) }
    var currentPage by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var initialLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var filters by remember { mutableStateOf(MovieListFilters()) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }

    // Counter incremented on filter changes to retrigger the
    // initial-load LaunchedEffect. Using filters directly as a key
    // works too, but a simple Int avoids deep-equality checks on
    // every recomposition.
    var filterKey by remember { mutableStateOf(0) }

    val pageSize = 20

    // ---- Load genres once ----
    LaunchedEffect(Unit) {
        MovieApi.getGenres().onSuccess { genres = it }
    }

    // ---- Initial load + filter-triggered reload ----
    LaunchedEffect(filterKey) {
        initialLoading = true
        error = null
        currentPage = 1
        hasMore = true
        movies = emptyList()

        val result = MovieApi.getMovies(filters = filters, page = 1, pageSize = pageSize)
        result.fold(
            onSuccess = { page ->
                movies = page.items
                hasMore = page.items.size >= pageSize
                error = null
            },
            onFailure = { e ->
                error = (e as? ApiException)?.userMessage
                    ?: e.message
                            ?: "An unknown error occurred."
            },
        )
        initialLoading = false
    }

    // ---- Infinite scroll listener ----
    DisposableEffect(Unit) {
        val listener: (org.w3c.dom.events.Event) -> Unit = {
            if (hasMore && !isLoadingMore && !initialLoading && error == null) {
                val scrollY = window.scrollY
                val windowHeight = window.innerHeight
                val bodyHeight = document.body?.scrollHeight ?: 0
                // Trigger when user is within 300px of the bottom.
                if (scrollY + windowHeight >= bodyHeight - 300) {
                    val nextPage = currentPage + 1
                    isLoadingMore = true
                    scope.launch {
                        val result = MovieApi.getMovies(
                            filters = filters,
                            page = nextPage,
                            pageSize = pageSize,
                        )
                        result.fold(
                            onSuccess = { page ->
                                movies = movies + page.items
                                currentPage = nextPage
                                hasMore = page.items.size >= pageSize
                            },
                            onFailure = {
                                // Silently ignore load-more failures.
                                // The user can scroll again to retry.
                            },
                        )
                        isLoadingMore = false
                    }
                }
            }
        }
        window.addEventListener("scroll", listener)
        onDispose { window.removeEventListener("scroll", listener) }
    }

    // ---- UI ----

    // Filters bar (always visible, even during loading).
    MovieFilters(
        genres = genres,
        currentFilters = filters,
        onFiltersChanged = { newFilters ->
            filters = newFilters
            filterKey += 1
        },
    )

    when {
        initialLoading -> {
            Div(attrs = { classes("movie-list-status") }) {
                P { Text("Cargando…") }
            }
        }

        error != null -> {
            Div(attrs = { classes("movie-list-status") }) {
                P(attrs = { classes("movie-list-error") }) {
                    Text("Error: $error")
                }
                Button(
                    attrs = {
                        classes("btn-primary")
                        onClick { filterKey += 1 }
                    },
                ) {
                    Text("Reintentar")
                }
            }
        }

        movies.isEmpty() -> {
            Div(attrs = { classes("movie-list-status") }) {
                P { Text("No se encontraron películas con estos filtros.") }
            }
        }

        else -> {
            MovieGrid(
                movies = movies,
                onMovieClick = { movie ->
                    AppState.navigateTo("movie/${movie.id}")
                },
            )

            if (isLoadingMore) {
                Div(attrs = { classes("movie-list-loading-more") }) {
                    P { Text("Cargando más películas…") }
                }
            }
        }
    }

    // ---- Back to top button (visible after scrolling down) ----
    var showBackToTop by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener: (org.w3c.dom.events.Event) -> Unit = {
            showBackToTop = window.scrollY > 600
        }
        window.addEventListener("scroll", listener)
        onDispose { window.removeEventListener("scroll", listener) }
    }

    if (showBackToTop) {
        Button(
            attrs = {
                classes("btn-back-to-top")
                onClick { window.scrollTo(0.0, 0.0) }
            },
        ) {
            Text("↑")
        }
    }
}

/**
 * Styles for MovieListScreen states (loading, error, empty, load-more).
 */
object MovieListScreenStyles : StyleSheet() {

    init {
        ".movie-list-status" style {
            property("display", "flex")
            property("flex-direction", "column")
            property("align-items", "center")
            property("justify-content", "center")
            property("min-height", "60vh")
            property("gap", "var(--space-4)")
            property("color", "var(--text-secondary)")
            property("font-size", "var(--font-size-lg)")
        }

        ".movie-list-error" style {
            property("color", "var(--text-primary)")
            property("text-align", "center")
            property("max-width", "600px")
        }

        ".movie-list-loading-more" style {
            property("display", "flex")
            property("justify-content", "center")
            property("padding", "var(--space-6)")
            property("color", "var(--text-secondary)")
        }

        ".btn-back-to-top" style {
            property("position", "fixed")
            property("bottom", "var(--space-6)")
            property("right", "var(--space-6)")
            property("width", "48px")
            property("height", "48px")
            property("border-radius", "50%")
            property("background-color", "var(--accent)")
            property("color", "var(--bg-primary)")
            property("border", "none")
            property("font-size", "var(--font-size-lg)")
            property("font-weight", "700")
            property("cursor", "pointer")
            property("display", "flex")
            property("align-items", "center")
            property("justify-content", "center")
            property("box-shadow", "0 4px 12px var(--shadow)")
            property("transition", "opacity 0.2s ease, transform 0.2s ease")
            property("z-index", "100")
        }

        ".btn-back-to-top:hover" style {
            property("background-color", "var(--accent-hover)")
            property("transform", "scale(1.1)")
        }

        ".search-header" style {
            property("padding", "var(--space-4) var(--space-4) 0")
        }

        ".search-header h2" style {
            property("color", "var(--text-primary)")
            property("font-size", "var(--font-size-xl)")
        }
    }
}