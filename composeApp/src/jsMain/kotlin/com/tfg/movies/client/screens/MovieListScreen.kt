package com.tfg.movies.client.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tfg.movies.client.api.ApiException
import com.tfg.movies.client.api.MovieApi
import com.tfg.movies.client.components.MovieGrid
import com.tfg.movies.shared.movies.MovieSummary
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

/**
 * Loading / data / error model for the movie list screen.
 *
 * Modelling the three mutually-exclusive states as a sealed class
 * eliminates the ambiguous combinations a boolean+nullable approach
 * allows ("loading=true while data already present", "error set
 * but loading still true", etc.). The when-exhaustiveness check
 * also forces the UI to render every state explicitly.
 */
sealed class MovieListState {
    object Loading : MovieListState()
    data class Success(val movies: List<MovieSummary>) : MovieListState()
    data class Error(val message: String) : MovieListState()
}

/**
 * Screen showing the movie catalog as a grid.
 *
 * On mount, fires a single GET /movies request via MovieApi and
 * renders one of three states:
 *
 *   - Loading: a centered "Cargando…" indicator while the request
 *     is in flight.
 *   - Success: the grid of MovieCards.
 *   - Error: a centered error message with a "Reintentar" button
 *     that re-triggers the fetch.
 *
 * The screen owns its state; later sub-steps (B7.4.5) will lift the
 * filters and pagination into this same screen, but for now it is
 * a self-contained loader.
 */
@Composable
fun MovieListScreen() {
    val scope = rememberCoroutineScope()
    var state: MovieListState by remember { mutableStateOf(MovieListState.Loading) }

    // Counter used as a key for LaunchedEffect; incrementing it
    // triggers a refetch. The "Reintentar" button relies on this
    // mechanism to avoid race conditions with the prior request.
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        state = MovieListState.Loading
        val result = MovieApi.getMovies(page = 1, pageSize = 20)
        state = result.fold(
            onSuccess = { page -> MovieListState.Success(page.items) },
            onFailure = { error ->
                val apiError = error as? ApiException
                val message = apiError?.userMessage
                    ?: error.message
                    ?: "An unknown error occurred."
                MovieListState.Error(message)
            },
        )
    }

    when (val s = state) {
        is MovieListState.Loading -> {
            Div(attrs = { classes("movie-list-status") }) {
                P { Text("Cargando…") }
            }
        }

        is MovieListState.Success -> {
            MovieGrid(s.movies)
        }

        is MovieListState.Error -> {
            Div(attrs = { classes("movie-list-status") }) {
                P(attrs = { classes("movie-list-error") }) {
                    Text("Error: ${s.message}")
                }
                Button(
                    attrs = {
                        classes("btn-primary")
                        onClick {
                            // Increment the reload key. LaunchedEffect
                            // observes it and will re-execute, which
                            // resets state to Loading and re-fetches.
                            reloadKey += 1
                        }
                    },
                ) {
                    Text("Reintentar")
                }
            }
        }
    }
}

/**
 * Styles for the loading and error placeholder containers.
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
    }
}