package com.tfg.movies.client

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tfg.movies.client.api.ApiException
import com.tfg.movies.client.api.MovieApi
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
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

    H1 { Text("TFG Movies — Recomendador") }
    P { Text("Hello World. Frontend up and running.") }

    // Temporary API smoke test from B7.3, kept until B7.4 introduces
    // the real movie listing screen.
    ApiSmokeTest()
}

@Composable
fun ApiSmokeTest() {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Idle.") }
    var loading by remember { mutableStateOf(false) }

    Div {
        Button(
            attrs = {
                classes("btn-primary")
                if (loading) {
                    attr("disabled", "true")
                }
                onClick {
                    loading = true
                    status = "Calling backend..."
                    scope.launch {
                        val result = MovieApi.getMovies(page = 1, pageSize = 5)
                        status = result.fold(
                            onSuccess = { page ->
                                "OK: received ${page.items.size} movies " +
                                        "(total ${page.total} in the catalog). " +
                                        "First: '${page.items.firstOrNull()?.title ?: "—"}'."
                            },
                            onFailure = { error ->
                                val api = error as? ApiException
                                if (api != null) {
                                    "ERROR ${api.status} [${api.errorCode}]: ${api.userMessage}"
                                } else {
                                    "ERROR: ${error.message ?: "unknown"}"
                                }
                            },
                        )
                        loading = false
                    }
                }
            }
        ) {
            Text(if (loading) "Loading..." else "Test API")
        }
    }

    P { Text(status) }
}