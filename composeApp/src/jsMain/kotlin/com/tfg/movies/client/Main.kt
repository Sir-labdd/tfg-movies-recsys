package com.tfg.movies.client

import androidx.compose.runtime.Composable
import com.tfg.movies.client.components.MovieCardStyles
import com.tfg.movies.client.components.MovieFiltersStyles
import com.tfg.movies.client.components.MovieGridStyles
import com.tfg.movies.client.screens.MovieDetailScreen
import com.tfg.movies.client.screens.MovieDetailScreenStyles
import com.tfg.movies.client.screens.MovieListScreen
import com.tfg.movies.client.screens.MovieListScreenStyles
import com.tfg.movies.client.state.AppState
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

fun main() {
    AppState.initPopStateListener()

    renderComposable(rootElementId = "root") {
        App()
    }
}

@Composable
fun App() {
    // Inject all stylesheets.
    Style(AppStyle)
    Style(MovieCardStyles)
    Style(MovieGridStyles)
    Style(MovieListScreenStyles)
    Style(MovieFiltersStyles)
    Style(MovieDetailScreenStyles)

    val route = AppState.currentRoute

    when {
        route.startsWith("movie/") -> {
            val id = route.removePrefix("movie/").toIntOrNull()
            if (id != null) {
                MovieDetailScreen(id)
            } else {
                // Invalid ID in URL — show listing.
                H1 { Text("TFG Movies — Recomendador") }
                MovieListScreen()
            }
        }

        else -> {
            H1 { Text("TFG Movies — Recomendador") }
            MovieListScreen()
        }
    }
}