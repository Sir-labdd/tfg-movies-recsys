package com.tfg.movies.server.movies

import com.tfg.movies.server.runWithApp
import com.tfg.movies.shared.errors.ErrorResponse
import com.tfg.movies.shared.movies.SimilarMoviesResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for GET /movies/{id}/similar.
 *
 * These tests run against the development database with embeddings
 * already generated. They assert on structural properties (correct
 * status codes, response shape, score ordering) rather than exact
 * recommendation contents, because the model's outputs may shift
 * slightly across regenerations and we want the suite to remain
 * stable.
 */
class MovieSimilarRoutesTest {

    @Test
    fun `returns 10 similar movies by default for an embedded movie`() = runWithApp { client ->
        val response = client.get("/movies/550/similar") // Fight Club

        assertEquals(HttpStatusCode.OK, response.status)

        val body: SimilarMoviesResponse = response.body()
        assertEquals(550, body.movieId)
        assertEquals(10, body.items.size, "Default limit should be 10")

        // The reference movie itself should never appear in the results.
        assertTrue(body.items.none { it.movie.id == 550 })

        // Similarities should be in descending order.
        val similarities = body.items.map { it.similarity }
        assertEquals(
            similarities.sortedDescending(),
            similarities,
            "Items should be ordered by similarity DESC",
        )

        // All similarities should be in the (0, 1] range.
        body.items.forEach { item ->
            assertTrue(
                item.similarity in 0.0..1.0,
                "Similarity ${item.similarity} out of [0,1] for movie ${item.movie.id}",
            )
        }
    }

    @Test
    fun `respects the limit parameter`() = runWithApp { client ->
        val response = client.get("/movies/550/similar?limit=5")

        assertEquals(HttpStatusCode.OK, response.status)

        val body: SimilarMoviesResponse = response.body()
        assertEquals(5, body.items.size)
    }

    @Test
    fun `minVoteCount filter narrows the result set`() = runWithApp { client ->
        // Without filter
        val without: SimilarMoviesResponse =
            client.get("/movies/550/similar?limit=10").body()

        // With a strong vote_count filter, fewer obscure movies pass.
        val with: SimilarMoviesResponse =
            client.get("/movies/550/similar?limit=10&minVoteCount=1000").body()

        // Every returned movie in the filtered set must have voteCount >= 1000.
        with.items.forEach { item ->
            val vc = item.movie.voteCount ?: 0
            assertTrue(
                vc >= 1000,
                "Movie ${item.movie.id} '${item.movie.title}' has voteCount=$vc, below threshold",
            )
        }

        // The two result sets are typically not identical.
        val idsWithout = without.items.map { it.movie.id }.toSet()
        val idsWith = with.items.map { it.movie.id }.toSet()
        assertTrue(
            idsWithout != idsWith,
            "Expected filtered and unfiltered results to differ",
        )
    }

    @Test
    fun `returns 404 for a movie without embedding`() = runWithApp { client ->
        val response = client.get("/movies/999999999/similar")

        assertEquals(HttpStatusCode.NotFound, response.status)

        val error: ErrorResponse = response.body()
        assertEquals("movie_not_found", error.error)
    }

    @Test
    fun `returns 400 for non-numeric id`() = runWithApp { client ->
        val response = client.get("/movies/abc/similar")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        val error: ErrorResponse = response.body()
        assertEquals("invalid_id", error.error)
    }

    @Test
    fun `returns 400 when limit exceeds max`() = runWithApp { client ->
        val response = client.get("/movies/550/similar?limit=500")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        val error: ErrorResponse = response.body()
        assertEquals("invalid_limit", error.error)
    }

    @Test
    fun `returns 400 when limit is zero or negative`() = runWithApp { client ->
        val response = client.get("/movies/550/similar?limit=0")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        val error: ErrorResponse = response.body()
        assertEquals("invalid_limit", error.error)
    }
}