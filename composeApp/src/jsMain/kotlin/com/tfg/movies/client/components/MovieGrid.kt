package com.tfg.movies.client.components

import androidx.compose.runtime.Composable
import com.tfg.movies.shared.movies.MovieSummary
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.dom.Div

/**
 * Responsive grid of [MovieCard]s.
 *
 * Uses CSS Grid with `repeat(auto-fill, minmax(180px, 1fr))` to
 * achieve a fluid layout:
 *
 *   - As many columns as can fit, given a minimum card width of 180px.
 *   - Columns stretch to fill any remaining horizontal space.
 *   - On a narrow viewport (< 380px) the grid collapses to one
 *     column without horizontal scroll.
 *
 * The grid itself takes 100% of the parent width and adds horizontal
 * padding so cards don't touch the viewport edges. The parent
 * Composable is responsible for placing MovieGrid inside a screen
 * layout (B7.4.5 will wrap it in a real screen container with
 * filters at the top).
 */
@Composable
fun MovieGrid(movies: List<MovieSummary>) {
    Div(attrs = { classes("movie-grid") }) {
        movies.forEach { movie ->
            MovieCard(movie)
        }
    }
}

/**
 * Styles for [MovieGrid]. Registered globally; injected in App() via
 * Style(MovieGridStyles).
 */
object MovieGridStyles : StyleSheet() {

    init {
        ".movie-grid" style {
            property("display", "grid")
            // Auto-fill: pack as many 180px-min cards as fit; share
            // remaining space equally (1fr each). This handles 1 to N
            // columns without media queries.
            property(
                "grid-template-columns",
                "repeat(auto-fill, minmax(180px, 1fr))",
            )
            property("gap", "var(--space-4)")
            property("padding", "var(--space-4)")
            property("width", "100%")
            property("box-sizing", "border-box")
        }
    }
}