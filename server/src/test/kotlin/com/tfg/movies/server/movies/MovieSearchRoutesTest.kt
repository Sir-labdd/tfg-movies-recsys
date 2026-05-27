package com.tfg.movies.server.movies

import com.tfg.movies.server.runWithApp
import com.tfg.movies.shared.errors.ErrorResponse
import com.tfg.movies.shared.movies.MovieSummary
import com.tfg.movies.shared.pagination.PaginatedResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for GET /movies/search.
 *
 * Searches anchor on movies known to exist in the dataset:
 * - "matrix": multiple Matrix films, exact match relevance.
 * - "amelie": French title with accent (tests unaccent index).
 * - "spider man": tests hyphen/space normalization (Spider-Man).
 */
class MovieSearchRoutesTest {

    @Test
    fun `simple query returns matching movies`() = runWithApp { client ->
        val response = client.get("/movies/search?q=matrix")

        assertEquals(HttpStatusCode.OK, response.status)

        val page: PaginatedResponse<MovieSummary> = response.body()
        assertTrue(page.items.isNotEmpty())
        // The Matrix (id=603) must be among results
        assertTrue(
            page.items.any { it.id == 603 },
            "Expected The Matrix (603) in results: ${page.items.map { it.id }}",
        )
    }

    @Test
    fun `relevance puts exact-title match first`() = runWithApp { client ->
        val response = client.get("/movies/search?q=matrix&pageSize=5")

        assertEquals(HttpStatusCode.OK, response.status)

        val page: PaginatedResponse<MovieSummary> = response.body()
        assertTrue(page.items.isNotEmpty())
        // The first item should be "The Matrix" (1999), not a sequel
        assertEquals(603, page.items.first().id, "Expected The Matrix first")
    }

    @Test
    fun `accent-insensitive query without accent finds title with accent`() = runWithApp { client ->
        val response = client.get("/movies/search?q=amelie")

        assertEquals(HttpStatusCode.OK, response.status)

        val page: PaginatedResponse<MovieSummary> = response.body()
        // Amélie (id=194) must be found despite the query lacking accent
        assertTrue(
            page.items.any { it.id == 194 },
            "Expected Amélie (194) in results for query 'amelie'",
        )
    }

    @Test
    fun `hyphen-insensitive 'spider man' finds 'Spider-Man'`() = runWithApp { client ->
        val response = client.get("/movies/search?q=spider%20man")

        assertEquals(HttpStatusCode.OK, response.status)

        val page: PaginatedResponse<MovieSummary> = response.body()
        // Spider-Man (id=557) must be found despite the query using space
        assertTrue(
            page.items.any { it.id == 557 },
            "Expected Spider-Man (557) for query 'spider man'",
        )
    }

    @Test
    fun `case-insensitive search`() = runWithApp { client ->
        val response = client.get("/movies/search?q=MATRIX&pageSize=5")
        assertEquals(HttpStatusCode.OK, response.status)

        val page: PaginatedResponse<MovieSummary> = response.body()
        assertTrue(page.items.any { it.id == 603 })
    }

    @Test
    fun `returns 400 when q is missing`() = runWithApp { client ->
        val response = client.get("/movies/search")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        val error: ErrorResponse = response.body()
        assertEquals("missing_query", error.error)
    }

    @Test
    fun `returns 400 when q is empty`() = runWithApp { client ->
        val response = client.get("/movies/search?q=")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        val error: ErrorResponse = response.body()
        assertEquals("missing_query", error.error)
    }

    @Test
    fun `returns 400 when q is too short`() = runWithApp { client ->
        val response = client.get("/movies/search?q=a")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        val error: ErrorResponse = response.body()
        assertEquals("invalid_search_query", error.error)
    }

    @Test
    fun `non-existent query returns empty page`() = runWithApp { client ->
        val response = client.get("/movies/search?q=xyzqwerty12345")

        assertEquals(HttpStatusCode.OK, response.status)

        val page: PaginatedResponse<MovieSummary> = response.body()
        assertTrue(page.items.isEmpty())
        assertEquals(0L, page.total)
    }
}