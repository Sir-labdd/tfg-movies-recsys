package com.tfg.movies.client.components

import androidx.compose.runtime.Composable
import com.tfg.movies.client.api.MovieListFilters
import com.tfg.movies.shared.movies.MovieSortBy
import com.tfg.movies.shared.movies.SortDirection
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLSelectElement

/**
 * Filter bar for the movie listing screen.
 *
 * Three dropdowns: genre, sort field, sort direction. Each change
 * calls [onFiltersChanged] with a new MovieListFilters instance,
 * which triggers a reload in MovieListScreen.
 */
@Composable
fun MovieFilters(
    genres: List<String>,
    currentFilters: MovieListFilters,
    onFiltersChanged: (MovieListFilters) -> Unit,
) {
    Div(attrs = { classes("movie-filters") }) {

        // ---- Genre dropdown ----
        Div(attrs = { classes("movie-filter-group") }) {
            Label { Text("Género") }
            Select(attrs = {
                onChange {
                    val value = (it.nativeEvent.target as? HTMLSelectElement)?.value ?: ""
                    onFiltersChanged(
                        currentFilters.copy(genre = value.ifEmpty { null })
                    )
                }
            }) {
                Option("", attrs = {}) { Text("Todos") }
                genres.forEach { genre ->
                    Option(genre, attrs = {
                        if (currentFilters.genre == genre) {
                            attr("selected", "")
                        }
                    }) { Text(genre) }
                }
            }
        }

        // ---- Sort field dropdown ----
        Div(attrs = { classes("movie-filter-group") }) {
            Label { Text("Ordenar por") }
            Select(attrs = {
                onChange {
                    val value = (it.nativeEvent.target as? HTMLSelectElement)?.value ?: ""
                    val sortBy = sortByFromLabel(value)
                    onFiltersChanged(currentFilters.copy(sortBy = sortBy))
                }
            }) {
                sortByOptions.forEach { (sortBy, label) ->
                    Option(label, attrs = {
                        if (currentFilters.sortBy == sortBy) {
                            attr("selected", "")
                        }
                    }) { Text(label) }
                }
            }
        }

        // ---- Sort direction dropdown ----
        Div(attrs = { classes("movie-filter-group") }) {
            Label { Text("Dirección") }
            Select(attrs = {
                onChange {
                    val value = (it.nativeEvent.target as? HTMLSelectElement)?.value ?: ""
                    val dir = if (value == "Ascendente") SortDirection.ASC else SortDirection.DESC
                    onFiltersChanged(currentFilters.copy(sortDir = dir))
                }
            }) {
                Option("Descendente", attrs = {
                    if (currentFilters.sortDir == SortDirection.DESC) attr("selected", "")
                }) { Text("Descendente") }
                Option("Ascendente", attrs = {
                    if (currentFilters.sortDir == SortDirection.ASC) attr("selected", "")
                }) { Text("Ascendente") }
            }
        }
    }
}

// ---- Helpers for sort-by mapping ----

private val sortByOptions: List<Pair<MovieSortBy, String>> = listOf(
    MovieSortBy.POPULARITY to "Popularidad",
    MovieSortBy.VOTE_AVERAGE to "Valoración",
    MovieSortBy.VOTE_COUNT to "Nº de votos",
    MovieSortBy.RELEASE_DATE to "Fecha de estreno",
    MovieSortBy.TITLE to "Título",
)

private fun sortByFromLabel(label: String): MovieSortBy {
    return sortByOptions.firstOrNull { it.second == label }?.first
        ?: MovieSortBy.POPULARITY
}

// ---- Styles ----

object MovieFiltersStyles : StyleSheet() {

    init {
        ".movie-filters" style {
            property("display", "flex")
            property("flex-wrap", "wrap")
            property("gap", "var(--space-4)")
            property("padding", "var(--space-4)")
            property("align-items", "flex-end")
        }

        ".movie-filter-group" style {
            property("display", "flex")
            property("flex-direction", "column")
            property("gap", "var(--space-1)")
        }

        ".movie-filter-group label" style {
            property("font-size", "var(--font-size-sm)")
            property("color", "var(--text-secondary)")
            property("font-weight", "500")
        }

        ".movie-filter-group select" style {
            property("font-family", "inherit")
            property("font-size", "var(--font-size-base)")
            property("padding", "var(--space-2) var(--space-4)")
            property("background-color", "var(--bg-secondary)")
            property("color", "var(--text-primary)")
            property("border", "1px solid var(--border)")
            property("border-radius", "var(--radius)")
            property("cursor", "pointer")
            property("min-width", "160px")
        }

        ".movie-filter-group select:focus" style {
            property("outline", "none")
            property("border-color", "var(--accent)")
        }
    }
}