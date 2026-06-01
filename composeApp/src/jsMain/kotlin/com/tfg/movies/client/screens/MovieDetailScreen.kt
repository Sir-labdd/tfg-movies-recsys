package com.tfg.movies.client.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tfg.movies.client.Constants
import com.tfg.movies.client.api.ApiException
import com.tfg.movies.client.api.MovieApi
import com.tfg.movies.client.components.MovieCard
import com.tfg.movies.client.state.AppState
import com.tfg.movies.shared.movies.MovieDetails
import com.tfg.movies.shared.movies.SimilarMovieItem
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Detail screen for a single movie.
 *
 * Fetches full movie information from GET /movies/{id} and semantic
 * recommendations from GET /movies/{id}/similar, then renders a
 * hero layout with poster, metadata, synopsis, cast grid and a
 * horizontal row of similar-movie cards.
 */
@Composable
fun MovieDetailScreen(movieId: Int) {
    var movie by remember { mutableStateOf<MovieDetails?>(null) }
    var similarMovies by remember { mutableStateOf<List<SimilarMovieItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Fetch movie details and similar movies in parallel.
    LaunchedEffect(movieId) {
        loading = true
        error = null

        // Details
        val detailResult = MovieApi.getMovie(movieId)
        detailResult.fold(
            onSuccess = { movie = it },
            onFailure = { e ->
                error = (e as? ApiException)?.userMessage
                    ?: e.message ?: "Unknown error"
            },
        )

        // Similar movies (fire even if detail succeeded)
        val similarResult = MovieApi.getSimilarMovies(movieId, limit = 10, minVoteCount = 50)
        similarResult.onSuccess { response ->
            similarMovies = response.items
        }

        loading = false
    }

    // ---- Back button ----
    Div(attrs = { classes("detail-back") }) {
        Button(
            attrs = {
                classes("btn-primary")
                onClick { AppState.goBack() }
            },
        ) {
            Text("← Volver")
        }
    }

    when {
        loading -> {
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
                        onClick { AppState.navigateTo("movie/$movieId") }
                    },
                ) {
                    Text("Reintentar")
                }
            }
        }

        movie != null -> {
            val m = movie!!
            DetailContent(m, similarMovies)
        }
    }
}

@Composable
private fun DetailContent(movie: MovieDetails, similarMovies: List<SimilarMovieItem>) {

    // ---- Hero section: poster + info ----
    Div(attrs = { classes("detail-hero") }) {

        // Poster
        Div(attrs = { classes("detail-poster") }) {
            val posterPath = movie.posterPath
            if (posterPath != null) {
                Img(
                    src = "${Constants.TMDB_IMAGE_BASE_URL}$posterPath",
                    alt = movie.title,
                ) {
                    attr(
                        "onerror",
                        "this.style.display='none';" +
                                "this.nextElementSibling.style.display='flex';",
                    )
                }
                Div(attrs = {
                    classes("movie-card-poster-fallback")
                    style { property("display", "none") }
                }) {
                    Span(attrs = { classes("movie-card-poster-icon") }) {
                        Text("\uD83C\uDFAC")
                    }
                }
            } else {
                Div(attrs = { classes("movie-card-poster-fallback") }) {
                    Span(attrs = { classes("movie-card-poster-icon") }) {
                        Text("\uD83C\uDFAC")
                    }
                }
            }
        }

        // Info column
        Div(attrs = { classes("detail-info") }) {

            H1 { Text(movie.title) }

            movie.tagline?.let { tagline ->
                if (tagline.isNotBlank()) {
                    P(attrs = { classes("detail-tagline") }) {
                        Text("\"$tagline\"")
                    }
                }
            }

            // Meta row: year, runtime, rating
            Div(attrs = { classes("detail-meta") }) {
                movie.releaseDate?.let { date ->
                    Span { Text(date.take(4)) } // year only
                }
                movie.runtime?.let { rt ->
                    if (rt > 0) Span { Text("${rt} min") }
                }
                movie.voteAverage?.let { avg ->
                    if ((movie.voteCount ?: 0) > 0) {
                        Span(attrs = { classes("detail-rating") }) {
                            val rounded = (avg * 10).toInt() / 10.0
                            Text("★ $rounded")
                        }
                    }
                }
                movie.voteCount?.let { count ->
                    if (count > 0) {
                        Span(attrs = { classes("detail-vote-count") }) {
                            Text("($count votos)")
                        }
                    }
                }
            }

            // Genres
            if (movie.genres.isNotEmpty()) {
                Div(attrs = { classes("movie-card-genres") }) {
                    movie.genres.forEach { genre ->
                        Span(attrs = { classes("movie-card-genre") }) {
                            Text(genre)
                        }
                    }
                }
            }

            // Overview
            movie.overview?.let { overview ->
                if (overview.isNotBlank()) {
                    Div(attrs = { classes("detail-section") }) {
                        H2 { Text("Sinopsis") }
                        P(attrs = { classes("detail-overview") }) {
                            Text(overview)
                        }
                    }
                }
            }
        }
    }

    // ---- Cast section ----
    if (movie.cast.isNotEmpty()) {
        Div(attrs = { classes("detail-section", "detail-section-full") }) {
            H2 { Text("Reparto") }
            Div(attrs = { classes("detail-cast-list") }) {
                movie.cast.take(12).forEach { member ->
                    Div(attrs = { classes("detail-cast-item") }) {
                        Span(attrs = { classes("detail-cast-name") }) {
                            Text(member.name)
                        }
                        member.character?.let { char ->
                            if (char.isNotBlank()) {
                                Span(attrs = { classes("detail-cast-character") }) {
                                    Text(char)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Similar movies section ----
    if (similarMovies.isNotEmpty()) {
        Div(attrs = { classes("detail-section", "detail-section-full") }) {
            H2 { Text("Películas similares") }
            Div(attrs = { classes("detail-similar-row") }) {
                similarMovies.forEach { item ->
                    Div(
                        attrs = {
                            classes("detail-similar-card")
                            onClick { AppState.navigateTo("movie/${item.movie.id}") }
                        },
                    ) {
                        MovieCard(item.movie)
                    }
                }
            }
        }
    }


}

// ---- Styles ----

object MovieDetailScreenStyles : StyleSheet() {
    init {
        ".detail-back" style {
            property("padding", "var(--space-4)")
        }

        ".detail-hero" style {
            property("display", "flex")
            property("gap", "var(--space-8)")
            property("padding", "var(--space-4)")
            property("flex-wrap", "wrap")
        }

        ".detail-poster" style {
            property("width", "300px")
            property("min-width", "200px")
            property("flex-shrink", "0")
            property("border-radius", "var(--radius-lg)")
            property("overflow", "hidden")
            property("background-color", "var(--bg-secondary)")
            property("aspect-ratio", "2 / 3")
        }

        ".detail-poster img" style {
            property("width", "100%")
            property("height", "100%")
            property("object-fit", "cover")
            property("display", "block")
        }

        ".detail-info" style {
            property("flex", "1")
            property("min-width", "280px")
            property("display", "flex")
            property("flex-direction", "column")
            property("gap", "var(--space-4)")
        }

        ".detail-info h1" style {
            property("font-size", "var(--font-size-xxl)")
            property("color", "var(--text-primary)")
        }

        ".detail-tagline" style {
            property("font-style", "italic")
            property("color", "var(--text-secondary)")
            property("font-size", "var(--font-size-lg)")
        }

        ".detail-meta" style {
            property("display", "flex")
            property("align-items", "center")
            property("gap", "var(--space-4)")
            property("color", "var(--text-secondary)")
            property("font-size", "var(--font-size-base)")
            property("flex-wrap", "wrap")
        }

        ".detail-rating" style {
            property("color", "var(--accent)")
            property("font-weight", "600")
        }

        ".detail-vote-count" style {
            property("color", "var(--text-muted)")
            property("font-size", "var(--font-size-sm)")
        }

        ".detail-section" style {
            property("padding", "0 var(--space-4)")
            property("margin-top", "var(--space-6)")
        }

        ".detail-section-full" style {
            property("padding", "0 var(--space-4)")
        }

        ".detail-section h2" style {
            property("font-size", "var(--font-size-xl)")
            property("color", "var(--text-primary)")
            property("margin-bottom", "var(--space-3)")
        }

        ".detail-overview" style {
            property("color", "var(--text-secondary)")
            property("line-height", "1.7")
            property("max-width", "800px")
        }

        // Cast
        ".detail-cast-list" style {
            property("display", "grid")
            property("grid-template-columns", "repeat(auto-fill, minmax(200px, 1fr))")
            property("gap", "var(--space-3)")
        }

        ".detail-cast-item" style {
            property("display", "flex")
            property("flex-direction", "column")
            property("gap", "2px")
        }

        ".detail-cast-name" style {
            property("color", "var(--text-primary)")
            property("font-weight", "500")
            property("font-size", "var(--font-size-sm)")
        }

        ".detail-cast-character" style {
            property("color", "var(--text-muted)")
            property("font-size", "var(--font-size-sm)")
        }

        // Similar movies — horizontal scrollable row
        ".detail-similar-row" style {
            property("display", "flex")
            property("gap", "var(--space-4)")
            property("overflow-x", "auto")
            property("padding", "var(--space-3) 0 var(--space-4) 0")
        }

        // Custom scrollbar for the similar-movies row
        ".detail-similar-row::-webkit-scrollbar" style {
            property("height", "6px")
        }

        ".detail-similar-row::-webkit-scrollbar-track" style {
            property("background", "var(--bg-secondary)")
            property("border-radius", "3px")
        }

        ".detail-similar-row::-webkit-scrollbar-thumb" style {
            property("background", "var(--border)")
            property("border-radius", "3px")
        }

        ".detail-similar-card" style {
            property("min-width", "180px")
            property("max-width", "180px")
            property("flex-shrink", "0")
            property("cursor", "pointer")
        }
    }
}