package com.tfg.movies.client

/**
 * Application-wide constants that don't fit elsewhere.
 */
object Constants {

    /**
     * Base URL for TMDB poster images. The `w300` segment selects the
     * 300-px-wide variant, which balances perceived sharpness against
     * download weight for grid-card sized renderings. Other available
     * widths include w92, w154, w185, w342, w500, w780 and original.
     *
     * To build a full poster URL, append the `posterPath` of a
     * MovieSummary (which already starts with a slash):
     *
     *     "$TMDB_IMAGE_BASE_URL${movie.posterPath}"
     *
     * The images are served by TMDB's CDN and require no API key for
     * read access.
     */
    const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w300"
}