package com.tfg.movies.server.movies

import com.tfg.movies.server.runWithApp
import com.tfg.movies.shared.errors.ErrorResponse
import com.tfg.movies.shared.movies.MovieDetails
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for GET /movies/{id}.
 *
 * Test data is anchored to Fight Club (TMDB id 550), which is a
 * stable, well-known entry in the dataset with rich relations
 * (multiple genres, countries, dozens of cast members, etc.).
 */
class MovieDetailsRoutesTest {

    @Test
    fun `returns 200 and full DTO for an existing movie`() = runWithApp { client ->
        val response = client.get("/movies/550") // Fight Club

        assertEquals(HttpStatusCode.OK, response.status)

        val movie: MovieDetails = response.body()
        assertEquals(550, movie.id)
        assertEquals("Fight Club", movie.title)
        assertEquals("en", movie.originalLanguage)
        assertEquals(1999, movie.releaseDate?.substring(0, 4)?.toInt())
        assertTrue(movie.genres.contains("Drama"), "Expected Drama in genres: ${movie.genres}")
        assertTrue(movie.cast.isNotEmpty(), "Expected non-empty cast")
        assertTrue(movie.crew.isNotEmpty(), "Expected non-empty crew")

        // Brad Pitt and Edward Norton must be in the cast — anchor the
        // assertion to specific person ids to be robust against future
        // changes in ordering or name normalization.
        val castIds = movie.cast.map { it.personId }
        assertTrue(287 in castIds, "Brad Pitt (287) should be in cast")
        assertTrue(819 in castIds, "Edward Norton (819) should be in cast")
    }

    @Test
    fun `returns 404 with structured error for a non-existing movie`() = runWithApp { client ->
        val response = client.get("/movies/999999999")

        assertEquals(HttpStatusCode.NotFound, response.status)

        val error: ErrorResponse = response.body()
        assertEquals("movie_not_found", error.error)
        assertTrue(error.message.contains("999999999"))
    }

    @Test
    fun `returns 400 with structured error for a non-numeric id`() = runWithApp { client ->
        val response = client.get("/movies/abc")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        val error: ErrorResponse = response.body()
        assertEquals("invalid_id", error.error)
        assertNotNull(error.message)
    }
}