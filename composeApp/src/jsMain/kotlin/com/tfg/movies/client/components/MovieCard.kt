package com.tfg.movies.client.components

import androidx.compose.runtime.Composable
import com.tfg.movies.client.Constants
import com.tfg.movies.shared.movies.MovieSummary
import org.jetbrains.compose.web.attributes.alt
import org.jetbrains.compose.web.attributes.src
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Visual card for one movie in a grid listing.
 *
 * Presentational only — the parent decides what happens on click (or
 * if anything happens at all). In B7.4 cards are static; click-to-
 * detail comes in B7.5.
 *
 * Structure:
 *   - Poster (2:3 ratio): image from TMDB, or a fallback box when
 *     posterPath is null.
 *   - Title: single line, ellipsised if too long.
 *   - Meta row: release year and vote average.
 *   - Genre pills: up to three visible.
 *
 * Styling lives in components/MovieCardStyles.kt to keep this file
 * focused on the structure.
 */
@Composable
fun MovieCard(movie: MovieSummary, onClick: (() -> Unit)? = null) {
    Div(attrs = {
        classes("movie-card")
        onClick?.let { handler -> onClick { handler() } }
    }) {

        // ---- Poster ----
        Div(attrs = { classes("movie-card-poster") }) {
            val posterPath = movie.posterPath
            if (posterPath != null) {
                // Render both the image and the fallback. CSS hides the
                // fallback by default. If the image fails to load (404,
                // expired path, CDN error), the inline onerror handler
                // hides the broken <img> and reveals the fallback sibling.
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
                // Hidden fallback — revealed by onerror above.
                Div(attrs = {
                    classes("movie-card-poster-fallback")
                    style { property("display", "none") }
                }) {
                    Span(attrs = { classes("movie-card-poster-icon") }) {
                        Text("\uD83C\uDFAC")
                    }
                    Span(attrs = { classes("movie-card-poster-fallback-title") }) {
                        Text(movie.title)
                    }
                }
            } else {
                // No poster path at all — show fallback directly.
                Div(attrs = { classes("movie-card-poster-fallback") }) {
                    Span(attrs = { classes("movie-card-poster-icon") }) {
                        Text("\uD83C\uDFAC")
                    }
                    Span(attrs = { classes("movie-card-poster-fallback-title") }) {
                        Text(movie.title)
                    }
                }
            }
        }

        // ---- Body (text content) ----
        Div(attrs = { classes("movie-card-body") }) {

            H3(attrs = { classes("movie-card-title") }) {
                Text(movie.title)
            }

            Div(attrs = { classes("movie-card-meta") }) {
                movie.year?.let { year ->
                    Span(attrs = { classes("movie-card-year") }) {
                        Text(year.toString())
                    }
                }
                movie.voteAverage?.let { rating ->
                    // Only show rating when there is at least one vote;
                    // a 0.0 average from zero votes is misleading.
                    if ((movie.voteCount ?: 0) > 0) {
                        Span(attrs = { classes("movie-card-rating") }) {
                            // Format to one decimal place. We do it
                            // manually because kotlin.js does not
                            // have a String.format equivalent.
                            val rounded = (rating * 10).toInt() / 10.0
                            Text("★ $rounded")
                        }
                    }
                }
            }

            // Genres — cap at 3 to keep the card compact.
            if (movie.genres.isNotEmpty()) {
                Div(attrs = { classes("movie-card-genres") }) {
                    movie.genres.take(3).forEach { genre ->
                        Span(attrs = { classes("movie-card-genre") }) {
                            Text(genre)
                        }
                    }
                }
            }
        }
    }
}