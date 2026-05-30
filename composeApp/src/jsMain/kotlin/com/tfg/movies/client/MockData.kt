package com.tfg.movies.client

import com.tfg.movies.shared.movies.MovieSummary

/**
 * Mock data used during B7.4.3 to validate the MovieGrid layout
 * before connecting to the real backend in B7.4.4. Removed once
 * the API integration is in place.
 *
 * Ten well-known movies with varied posters, vote counts and genres,
 * so layout edge cases (long titles, multiple genres, low/high
 * ratings, missing poster) are exercised.
 *
 * Poster paths are real TMDB paths and are served by their CDN with
 * no API key required.
 */
object MockData {

    val sampleMovies: List<MovieSummary> = listOf(
        MovieSummary(
            id = 550,
            title = "Fight Club",
            year = 1999,
            posterPath = "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
            voteAverage = 8.4,
            voteCount = 27543,
            popularity = 70.5,
            genres = listOf("Drama", "Thriller"),
        ),
        MovieSummary(
            id = 27205,
            title = "Inception",
            year = 2010,
            posterPath = "/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg",
            voteAverage = 8.4,
            voteCount = 34921,
            popularity = 85.2,
            genres = listOf("Action", "Science Fiction", "Adventure"),
        ),
        MovieSummary(
            id = 862,
            title = "Toy Story",
            year = 1995,
            posterPath = "/uXDfjJbdP4ijW5hWSBrPrlKpxab.jpg",
            voteAverage = 8.0,
            voteCount = 17823,
            popularity = 60.1,
            genres = listOf("Animation", "Comedy", "Family"),
        ),
        MovieSummary(
            id = 13,
            title = "Forrest Gump",
            year = 1994,
            posterPath = "/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg",
            voteAverage = 8.5,
            voteCount = 28456,
            popularity = 65.8,
            genres = listOf("Comedy", "Drama", "Romance"),
        ),
        MovieSummary(
            id = 155,
            title = "The Dark Knight",
            year = 2008,
            posterPath = "/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
            voteAverage = 8.5,
            voteCount = 32987,
            popularity = 95.4,
            genres = listOf("Drama", "Action", "Crime", "Thriller"),
        ),
        MovieSummary(
            id = 680,
            title = "Pulp Fiction",
            year = 1994,
            posterPath = "/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg",
            voteAverage = 8.5,
            voteCount = 27654,
            popularity = 62.3,
            genres = listOf("Thriller", "Crime"),
        ),
        MovieSummary(
            id = 238,
            title = "The Godfather",
            year = 1972,
            posterPath = "/3bhkrj58Vtu7enYsRolD1fZdja1.jpg",
            voteAverage = 8.7,
            voteCount = 20198,
            popularity = 55.7,
            genres = listOf("Drama", "Crime"),
        ),
        MovieSummary(
            id = 769,
            title = "GoodFellas",
            year = 1990,
            posterPath = "/aKuFiU82s5ISJpGZp7YkIr3kCUd.jpg",
            voteAverage = 8.5,
            voteCount = 12087,
            popularity = 48.9,
            genres = listOf("Drama", "Crime"),
        ),
        MovieSummary(
            id = 12477,
            title = "Grave of the Fireflies",
            year = 1988,
            posterPath = "/k9tv1rXZbOhH7eiCk378x61kNQ1.jpg",
            voteAverage = 8.5,
            voteCount = 4321,
            popularity = 32.1,
            genres = listOf("Animation", "Drama", "War"),
        ),
        // One entry without a poster, to exercise the fallback rendering.
        MovieSummary(
            id = 999999,
            title = "Mock Movie Without Poster (For Testing The Fallback Box)",
            year = 2024,
            posterPath = null,
            voteAverage = 6.5,
            voteCount = 42,
            popularity = 1.2,
            genres = listOf("Drama"),
        ),
    )
}