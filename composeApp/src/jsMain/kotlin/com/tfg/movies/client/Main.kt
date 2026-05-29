package com.tfg.movies.client

import androidx.compose.runtime.Composable
import com.tfg.movies.client.components.MovieCardStyles
import com.tfg.movies.client.components.MovieGridStyles
import com.tfg.movies.client.screens.MovieListScreen
import com.tfg.movies.client.screens.MovieListScreenStyles
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
fun main() {
    renderComposable(rootElementId = "root") {
        App()
    }
}

@Composable
fun App() {
    // Inject the application's design system (CSS custom properties
    // and base resets). Style{} renders a <style> tag whose content
    // is the result of compiling AppStyle's StyleSheet to CSS text.
      Style(AppStyle)
    Style(MovieCardStyles)
    Style(MovieGridStyles)
    Style(MovieListScreenStyles)

    H1 { Text("TFG Movies — Recomendador") }

    MovieListScreen()
}