package com.tfg.movies.client.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tfg.movies.client.Constants
import com.tfg.movies.client.api.MovieApi
import com.tfg.movies.client.state.AppState
import com.tfg.movies.shared.movies.MovieSummary
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLInputElement

/**
 * Persistent navigation bar with live search autocomplete.
 *
 * As the user types (2+ characters), a debounced search fires after
 * 300ms of inactivity. Results appear in a dropdown overlay below
 * the input. Clicking a result navigates to the detail screen and
 * closes the dropdown. Pressing Enter or the "Buscar" button
 * navigates to the full search results screen.
 */
@Composable
fun NavBar() {
    var searchText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<MovieSummary>>(emptyList()) }
    var showDropdown by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    // Debounced live search: fires 300ms after the user stops typing.
    LaunchedEffect(searchText) {
        if (searchText.length < 2) {
            suggestions = emptyList()
            showDropdown = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(300) // debounce
        val result = MovieApi.searchMovies(query = searchText.trim(), page = 1, pageSize = 8)
        result.fold(
            onSuccess = { page ->
                suggestions = page.items
                showDropdown = page.items.isNotEmpty()
            },
            onFailure = {
                suggestions = emptyList()
                showDropdown = false
            },
        )
        isSearching = false
    }

    Div(attrs = { classes("navbar") }) {
        // ---- Title / home link ----
        H1(attrs = {
            classes("navbar-title")
            onClick { AppState.navigateTo("movies") }
        }) {
            Text("TFG Movies")
        }

        // ---- Search with autocomplete ----
        Div(attrs = { classes("navbar-search") }) {
            Input(InputType.Text) {
                classes("navbar-search-input")
                placeholder("Buscar películas…")
                value(searchText)
                onInput { event ->
                    searchText = (event.nativeEvent.target as? HTMLInputElement)?.value ?: ""
                }
                onKeyDown { event ->
                    if (event.key == "Enter" && searchText.isNotBlank()) {
                        showDropdown = false
                        AppState.navigateTo("search/${searchText.trim()}")
                    }
                    if (event.key == "Escape") {
                        showDropdown = false
                    }
                }
                onFocusIn {
                    if (suggestions.isNotEmpty()) showDropdown = true
                }
            }
            Button(
                attrs = {
                    classes("btn-primary", "navbar-search-btn")
                    onClick {
                        if (searchText.isNotBlank()) {
                            showDropdown = false
                            AppState.navigateTo("search/${searchText.trim()}")
                        }
                    }
                },
            ) {
                Text("Buscar")
            }

            // ---- Autocomplete dropdown ----
            if (showDropdown) {
                Div(attrs = { classes("search-dropdown") }) {
                    suggestions.forEach { movie ->
                        Div(
                            attrs = {
                                classes("search-dropdown-item")
                                onClick {
                                    showDropdown = false
                                    searchText = ""
                                    AppState.navigateTo("movie/${movie.id}")
                                }
                            },
                        ) {
                            // Mini poster
                            Div(attrs = { classes("search-dropdown-poster") }) {
                                val posterPath = movie.posterPath
                                if (posterPath != null) {
                                    Img(
                                        src = "${Constants.TMDB_IMAGE_BASE_URL}$posterPath",
                                        alt = "",
                                    ) {
                                        attr(
                                            "onerror",
                                            "this.style.display='none'",
                                        )
                                    }
                                }
                            }

                            // Info
                            Div(attrs = { classes("search-dropdown-info") }) {
                                Span(attrs = { classes("search-dropdown-title") }) {
                                    Text(movie.title)
                                }
                                Span(attrs = { classes("search-dropdown-meta") }) {
                                    val parts = mutableListOf<String>()
                                    movie.year?.let { parts.add(it.toString()) }
                                    movie.voteAverage?.let { avg ->
                                        if ((movie.voteCount ?: 0) > 0) {
                                            parts.add("★ ${(avg * 10).toInt() / 10.0}")
                                        }
                                    }
                                    if (movie.genres.isNotEmpty()) {
                                        parts.add(movie.genres.take(2).joinToString(", "))
                                    }
                                    Text(parts.joinToString(" · "))
                                }
                            }
                        }
                    }

                    // Footer: link to full results
                    Div(
                        attrs = {
                            classes("search-dropdown-footer")
                            onClick {
                                showDropdown = false
                                AppState.navigateTo("search/${searchText.trim()}")
                            }
                        },
                    ) {
                        Text("Ver todos los resultados →")
                    }
                }
            }
        }
    }
}

// ---- Styles ----

object NavBarStyles : StyleSheet() {
    init {
        ".navbar" style {
            property("display", "flex")
            property("align-items", "center")
            property("justify-content", "space-between")
            property("padding", "var(--space-3) var(--space-4)")
            property("background-color", "var(--bg-secondary)")
            property("border-bottom", "1px solid var(--border)")
            property("flex-wrap", "wrap")
            property("gap", "var(--space-3)")
            property("position", "sticky")
            property("top", "0")
            property("z-index", "50")
        }

        ".navbar-title" style {
            property("font-size", "var(--font-size-xl)")
            property("color", "var(--accent)")
            property("cursor", "pointer")
            property("margin", "0")
            property("white-space", "nowrap")
        }

        ".navbar-title:hover" style {
            property("opacity", "0.8")
        }

        ".navbar-search" style {
            property("display", "flex")
            property("gap", "var(--space-2)")
            property("flex", "1")
            property("max-width", "500px")
            property("min-width", "200px")
            property("position", "relative") // anchor for dropdown
            property("flex-wrap", "wrap")
        }

        ".navbar-search-input" style {
            property("flex", "1")
            property("font-family", "inherit")
            property("font-size", "var(--font-size-base)")
            property("padding", "var(--space-2) var(--space-3)")
            property("background-color", "var(--bg-primary)")
            property("color", "var(--text-primary)")
            property("border", "1px solid var(--border)")
            property("border-radius", "var(--radius)")
            property("outline", "none")
            property("min-width", "150px")
        }

        ".navbar-search-input:focus" style {
            property("border-color", "var(--accent)")
        }

        ".navbar-search-input::placeholder" style {
            property("color", "var(--text-muted)")
        }

        ".navbar-search-btn" style {
            property("white-space", "nowrap")
        }

        // ---- Autocomplete dropdown ----
        ".search-dropdown" style {
            property("position", "absolute")
            property("top", "100%")
            property("left", "0")
            property("right", "0")
            property("background-color", "var(--bg-secondary)")
            property("border", "1px solid var(--border)")
            property("border-top", "none")
            property("border-radius", "0 0 var(--radius) var(--radius)")
            property("max-height", "400px")
            property("overflow-y", "auto")
            property("z-index", "100")
            property("box-shadow", "0 8px 24px var(--shadow)")
        }

        ".search-dropdown-item" style {
            property("display", "flex")
            property("align-items", "center")
            property("gap", "var(--space-3)")
            property("padding", "var(--space-2) var(--space-3)")
            property("cursor", "pointer")
            property("transition", "background-color 0.1s ease")
        }

        ".search-dropdown-item:hover" style {
            property("background-color", "var(--bg-tertiary)")
        }

        ".search-dropdown-poster" style {
            property("width", "40px")
            property("height", "60px")
            property("flex-shrink", "0")
            property("background-color", "var(--bg-tertiary)")
            property("border-radius", "4px")
            property("overflow", "hidden")
        }

        ".search-dropdown-poster img" style {
            property("width", "100%")
            property("height", "100%")
            property("object-fit", "cover")
        }

        ".search-dropdown-info" style {
            property("display", "flex")
            property("flex-direction", "column")
            property("gap", "2px")
            property("overflow", "hidden")
        }

        ".search-dropdown-title" style {
            property("color", "var(--text-primary)")
            property("font-weight", "500")
            property("white-space", "nowrap")
            property("overflow", "hidden")
            property("text-overflow", "ellipsis")
        }

        ".search-dropdown-meta" style {
            property("color", "var(--text-muted)")
            property("font-size", "var(--font-size-sm)")
        }

        ".search-dropdown-footer" style {
            property("padding", "var(--space-3)")
            property("text-align", "center")
            property("color", "var(--accent)")
            property("cursor", "pointer")
            property("font-size", "var(--font-size-sm)")
            property("border-top", "1px solid var(--border)")
        }

        ".search-dropdown-footer:hover" style {
            property("background-color", "var(--bg-tertiary)")
        }

        // Custom scrollbar for the dropdown
        ".search-dropdown::-webkit-scrollbar" style {
            property("width", "6px")
        }

        ".search-dropdown::-webkit-scrollbar-track" style {
            property("background", "var(--bg-secondary)")
        }

        ".search-dropdown::-webkit-scrollbar-thumb" style {
            property("background", "var(--border)")
            property("border-radius", "3px")
        }
    }
}