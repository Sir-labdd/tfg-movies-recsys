package com.tfg.movies.client.components

import org.jetbrains.compose.web.css.StyleSheet

/**
 * Styles for [MovieCard].
 *
 * Kept as a separate StyleSheet so MovieCard.kt stays focused on
 * structure. The styles are registered globally (not scoped to a
 * particular Composable) so they only need to be injected once at
 * the application root.
 *
 * Naming: BEM-ish (movie-card, movie-card-poster, movie-card-title)
 * for readability when inspecting the DOM in DevTools.
 */
object MovieCardStyles : StyleSheet() {

    init {
        // -------- Card container --------
        ".movie-card" style {
            property("background-color", "var(--bg-secondary)")
            property("border", "1px solid var(--border)")
            property("border-radius", "var(--radius-lg)")
            property("overflow", "hidden")
            property("display", "flex")
            property("flex-direction", "column")
            property("transition", "transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease")
            property("cursor", "pointer")
        }

        ".movie-card:hover" style {
            property("transform", "translateY(-2px)")
            property("box-shadow", "0 8px 16px var(--shadow)")
            property("border-color", "var(--accent)")
        }

        // -------- Poster section --------
        ".movie-card-poster" style {
            property("width", "100%")
            // Aspect ratio 2:3 — the standard movie poster proportion.
            property("aspect-ratio", "2 / 3")
            property("background-color", "var(--bg-tertiary)")
            property("position", "relative")
            property("overflow", "hidden")
        }

        ".movie-card-poster img" style {
            property("width", "100%")
            property("height", "100%")
            property("object-fit", "cover")
            property("display", "block")
        }

        // -------- Fallback for movies without poster --------
        ".movie-card-poster-fallback" style {
            property("width", "100%")
            property("height", "100%")
            property("display", "flex")
            property("flex-direction", "column")
            property("align-items", "center")
            property("justify-content", "center")
            property("padding", "var(--space-4)")
            property("box-sizing", "border-box")
            property("background", "linear-gradient(135deg, var(--bg-tertiary), var(--bg-secondary))")
        }

        ".movie-card-poster-icon" style {
            property("font-size", "48px")
            property("margin-bottom", "var(--space-2)")
            property("opacity", "0.4")
        }

        ".movie-card-poster-fallback-title" style {
            property("font-size", "var(--font-size-sm)")
            property("color", "var(--text-muted)")
            property("text-align", "center")
            // Up to three lines, then ellipsis.
            property("display", "-webkit-box")
            property("-webkit-line-clamp", "3")
            property("-webkit-box-orient", "vertical")
            property("overflow", "hidden")
        }

        // -------- Body (text content) --------
        ".movie-card-body" style {
            property("padding", "var(--space-3)")
            property("display", "flex")
            property("flex-direction", "column")
            property("gap", "var(--space-2)")
        }

        ".movie-card-title" style {
            property("font-size", "var(--font-size-base)")
            property("font-weight", "600")
            property("color", "var(--text-primary)")
            property("margin", "0")
            // Single line with ellipsis if too long.
            property("white-space", "nowrap")
            property("overflow", "hidden")
            property("text-overflow", "ellipsis")
        }

        // -------- Meta row (year + rating) --------
        ".movie-card-meta" style {
            property("display", "flex")
            property("align-items", "center")
            property("gap", "var(--space-3)")
            property("font-size", "var(--font-size-sm)")
            property("color", "var(--text-secondary)")
        }

        ".movie-card-rating" style {
            property("color", "var(--accent)")
            property("font-weight", "500")
        }

        // -------- Genre pills --------
        ".movie-card-genres" style {
            property("display", "flex")
            property("flex-wrap", "wrap")
            property("gap", "var(--space-1)")
        }

        ".movie-card-genre" style {
            property("font-size", "11px")
            property("padding", "2px var(--space-2)")
            property("background-color", "var(--bg-tertiary)")
            property("color", "var(--text-secondary)")
            property("border-radius", "var(--radius)")
            property("text-transform", "uppercase")
            property("letter-spacing", "0.03em")
        }
    }
}