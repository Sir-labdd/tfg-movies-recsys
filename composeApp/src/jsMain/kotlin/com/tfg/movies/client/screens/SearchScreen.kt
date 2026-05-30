package com.tfg.movies.client.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tfg.movies.client.api.ApiException
import com.tfg.movies.client.api.MovieApi
import com.tfg.movies.client.components.MovieGrid
import com.tfg.movies.client.state.AppState
import com.tfg.movies.shared.movies.MovieSummary
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

/**
 * Search results screen.
 *
 * Fetches results from GET /movies/search?q={query} and displays
 * them as a MovieGrid. Reuses the same card and grid components
 * as the listing screen.
 */
@Composable
fun SearchScreen(query: String) {
    var movies by remember(query) { mutableStateOf<List<MovieSummary>>(emptyList()) }
    var loading by remember(query) { mutableStateOf(true) }
    var error by remember(query) { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        loading = true
        error = null
        val result = MovieApi.searchMovies(query = query, page = 1, pageSize = 50)
        result.fold(
            onSuccess = { page ->
                movies = page.items
                error = null
            },
            onFailure = { e ->
                error = (e as? ApiException)?.userMessage
                    ?: e.message ?: "Unknown error"
            },
        )
        loading = false
    }

    Div(attrs = { classes("search-header") }) {
        H2 { Text("Resultados para \"$query\"") }
    }

    when {
        loading -> {
            Div(attrs = { classes("movie-list-status") }) {
                P { Text("Buscando…") }
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
                        onClick { AppState.navigateTo("search/$query") }
                    },
                ) {
                    Text("Reintentar")
                }
            }
        }

        movies.isEmpty() -> {
            Div(attrs = { classes("movie-list-status") }) {
                P { Text("No se encontraron películas para \"$query\".") }
            }
        }

        else -> {
            MovieGrid(
                movies = movies,
                onMovieClick = { movie ->
                    AppState.navigateTo("movie/${movie.id}")
                },
            )
        }
    }
}