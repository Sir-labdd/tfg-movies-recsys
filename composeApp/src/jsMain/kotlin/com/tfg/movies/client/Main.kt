package com.tfg.movies.client

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

/**
 * Entry point of the web client.
 *
 * The Compose Multiplatform plugin invokes this main() at module load
 * time. It locates the DOM element with id="root" (declared in
 * src/jsMain/resources/index.html) and renders the Compose tree below
 * it. Any change to App() during development is automatically
 * recompiled and hot-reloaded by the dev server.
 */
fun main() {
    renderComposable(rootElementId = "root") {
        App()
    }
}

@Composable
fun App() {
    H1 { Text("TFG Movies — Recomendador") }
    P { Text("Hello World. Frontend up and running.") }
}