package com.tfg.movies.client

import org.jetbrains.compose.web.css.*

/**
 * Application-wide design tokens implemented as CSS custom properties.
 *
 * Two-layer model:
 *
 *   1. `AppStyle` injects the design tokens (colors, fonts, spacing)
 *      onto :root as CSS variables. This is the "design system layer":
 *      a single source of truth that the rest of the stylesheet, and
 *      any inline styles, can reference via var(--xxx).
 *
 *   2. The same StyleSheet also applies base resets to `body` so the
 *      whole page picks up the dark palette immediately, without any
 *      Composable having to opt in.
 *
 * Why CSS variables instead of Kotlin constants:
 *   - Dark/light toggle (planned for B7.7) becomes a one-liner: swap
 *     the values on :root by toggling a class on <body>. No Compose
 *     recomposition required.
 *   - Variables are visible in the browser's DevTools and can be
 *     edited live during development.
 *   - Inline styles in components can reference var(--xxx) directly,
 *     so we don't need to thread color parameters down the tree.
 */
object AppStyle : StyleSheet() {

    init {
        // ----------------------------------------------------------------
        // Design tokens (CSS custom properties).
        // Applied to :root so they are inherited by every element.
        // ----------------------------------------------------------------
        ":root" style {
            // Backgrounds — three layers of dark, from deepest to lightest.
            property("--bg-primary", "#0d1117")
            property("--bg-secondary", "#161b22")
            property("--bg-tertiary", "#21262d")

            // Text — three levels of emphasis on top of the dark backgrounds.
            property("--text-primary", "#e6edf3")
            property("--text-secondary", "#8b949e")
            property("--text-muted", "#6e7681")

            // Accent — amber, used sparingly on interactive elements
            // and key highlights. Avoids the Netflix-red cliché.
            property("--accent", "#a78bfa")
            property("--accent-hover", "#c4b5fd")

            // Borders and shadows — subtle separators between surfaces.
            property("--border", "#30363d")
            property("--shadow", "rgba(0, 0, 0, 0.4)")

            // Typography. System fonts only — no external font requests.
            property(
                "--font-family",
                "system-ui, -apple-system, \"Segoe UI\", Roboto, sans-serif",
            )
            property("--font-size-base", "16px")
            property("--font-size-sm", "14px")
            property("--font-size-lg", "20px")
            property("--font-size-xl", "28px")
            property("--font-size-xxl", "40px")

            // Spacing scale — multiples of 4px, the modern web standard.
            property("--space-1", "4px")
            property("--space-2", "8px")
            property("--space-3", "12px")
            property("--space-4", "16px")
            property("--space-6", "24px")
            property("--space-8", "32px")
            property("--space-12", "48px")

            // Radii — for cards and buttons.
            property("--radius", "8px")
            property("--radius-lg", "12px")
        }

        // ----------------------------------------------------------------
        // Base resets.
        // Apply the design tokens to the html/body so the dark palette
        // takes effect immediately, before any Composable mounts.
        // ----------------------------------------------------------------
        "html, body" style {
            margin(0.px)
            padding(0.px)
            backgroundColor(Color("var(--bg-primary)"))
            color(Color("var(--text-primary)"))
            property("font-family", "var(--font-family)")
            property("font-size", "var(--font-size-base)")
            lineHeight("1.5")
            property("-webkit-font-smoothing", "antialiased")
            property("text-rendering", "optimizeLegibility")
        }

        // Sensible defaults for the most common elements so we don't
        // have to override them in every screen.
        "h1, h2, h3, h4, h5, h6" style {
            margin(0.px)
            property("font-weight", "600")
            lineHeight("1.2")
        }

        "h1" style {
            property("font-size", "var(--font-size-xxl)")
        }

        "h2" style {
            property("font-size", "var(--font-size-xl)")
        }

        "p" style {
            margin(0.px)
            color(Color("var(--text-secondary)"))
        }

        "a" style {
            color(Color("var(--accent)"))
            property("text-decoration", "none")
        }

        "a:hover" style {
            color(Color("var(--accent-hover)"))
        }

        // The Compose root element. Becomes a flex container so children
        // can use the full viewport height when needed.
        "#root" style {
            property("min-height", "100vh")
            property("display", "flex")
            property("flex-direction", "column")
        }

        // ----------------------------------------------------------------
        // Buttons. Three variants:
        //   - default <button>: secondary action ("ghost" style with
        //     amber border on hover).
        //   - .btn-primary: primary action (amber filled).
        //   - .btn-disabled state: applies to both.
        // ----------------------------------------------------------------
        "button" style {
            property("font-family", "inherit")
            property("font-size", "var(--font-size-base)")
            property("padding", "var(--space-2) var(--space-4)")
            property("background-color", "transparent")
            property("color", "var(--text-primary)")
            property("border", "1px solid var(--text-secondary)")
            property("border-radius", "var(--radius)")
            property("cursor", "pointer")
            property("transition", "background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease")
        }

        "button:hover" style {
            property("background-color", "var(--bg-tertiary)")
            property("border-color", "var(--accent)")
            property("color", "var(--accent)")
        }

        "button:disabled" style {
            property("opacity", "0.5")
            property("cursor", "not-allowed")
        }

        "button:disabled:hover" style {
            // Don't change colors on hover when disabled.
            property("background-color", "transparent")
            property("border-color", "var(--text-secondary)")
            property("color", "var(--text-primary)")
        }

        // Primary action button — filled amber, used for the main
        // action on each screen.
        ".btn-primary" style {
            property("background-color", "var(--accent)")
            property("color", "var(--bg-primary)")
            property("border-color", "var(--accent)")
            property("font-weight", "600")
        }

        ".btn-primary:hover" style {
            property("background-color", "var(--accent-hover)")
            property("border-color", "var(--accent-hover)")
            property("color", "var(--bg-primary)")
        }
    }
}