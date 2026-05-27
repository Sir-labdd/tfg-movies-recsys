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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Integration tests for GET /movies (paginated listing).
 *
 * Tests assert on structural and behavioral properties (correct
 * status codes, page sizes, filter semantics) rather than on exact
 * row contents, so they remain stable if popularity scores or other
 * orderings shift slightly.
 */
class MovieListRoutesTest {

    @Test
    fun `returns first page with default page size when no params given`() = runWithApp { client ->
        val response = client.get("/movies")

        assertEquals(HttpStatusCode.OK, response.status)

        val page: PaginatedResponse<MovieSummary> = response.body()
        assertEquals(1, page.page)
        assertEquals(20, page.pageSize)
        assertEquals(20, page.items.size)
        assertTrue(page.total > 40_000, "Expected ~45,408 total movies, got ${page.total}")
    }

    @Test
    fun `respects pageSize parameter`() = runWithApp { client ->
        val response = client.get("/movies?pageSize=5")

        assertEquals(HttpStatusCode.OK, response.status)

        val page: PaginatedResponse<MovieSummary> = response.body()
        assertEquals(5, page.pageSize)
        assertEquals(5, page.items.size)
    }

    @Test
    fun `paginating returns different items per page`() = runWithApp { client ->
        val page1: PaginatedResponse<MovieSummary> =
            client.get("/movies?pageSize=10&page=1").body()
        val page2: PaginatedResponse<MovieSummary> =
            client.get("/movies?pageSize=10&page=2").body()

        val ids1 = page1.items.map { it.id }.toSet()
        val ids2 = page2.items.map { it.id }.toSet()

        assertTrue(ids1.isNotEmpty())
        assertTrue(ids2.isNotEmpty())
        assertTrue(ids1.intersect(ids2).isEmpty(), "Pages should not overlap")
    }

    @Test
    fun `filter by genre returns only matching movies`() = runWithApp { client ->
        val response = client.get("/movies?genre=Animation&pageSize=20")

        assertEquals(HttpStatusCode.OK, response.status)

        val page: PaginatedResponse<MovieSummary> = response.body()
        assertTrue(page.items.isNotEmpty(), "Expected animation movies in dataset")
        page.items.forEach { movie ->
            assertTrue(
                "Animation" in movie.genres,
                "Movie ${movie.id} '${movie.title}' has genres ${movie.genres} but Animation was expected",
            )
        }
    }

    @Test
    fun `filter by year returns only movies of that year`() = runWithApp { client ->
        val response = client.get("/movies?year=1999&pageSize=10")

        assertEquals(HttpStatusCode.OK, response.status)

        val page: PaginatedResponse<MovieSummary> = response.body()
        assertTrue(page.items.isNotEmpty())
        page.items.forEach { movie ->
            assertEquals(1999, movie.year, "Movie ${movie.title} has year ${movie.year}")
        }
    }

    @Test
    fun `combined filters narrow the result set`() = runWithApp { client ->
        val response = client.get(
            "/movies?genre=Drama&yearFrom=1990&yearTo=1999&minVoteCount=500" +
                    "&sortBy=voteAverage&pageSize=5"
        )

        assertEquals(HttpStatusCode.OK, response.status)

        val page: PaginatedResponse<MovieSummary> = response.body()
        assertTrue(page.items.isNotEmpty(), "Expected at least one drama from the 90s")
        page.items.forEach { movie ->
            assertTrue("Drama" in movie.genres, "Movie ${movie.title} not Drama")
            val year = movie.year ?: -1
            assertTrue(year in 1990..1999, "Movie ${movie.title} year=$year out of range")
            assertTrue((movie.voteCount ?: 0) >= 500, "Movie ${movie.title} below voteCount threshold")
        }
    }

    @Test
    fun `sort by voteAverage puts higher rated movies first`() = runWithApp { client ->
        // Auto minVoteCount=10 default kicks in when sorting by voteAverage,
        // so the top should be high-rated movies with at least some votes.
        val response = client.get("/movies?sortBy=voteAverage&pageSize=10")

        assertEquals(HttpStatusCode.OK, response.status)

        val page: PaginatedResponse<MovieSummary> = response.body()
        val averages = page.items.mapNotNull { it.voteAverage }
        assertTrue(averages.isNotEmpty())
        // First item should have voteAverage >= last item's voteAverage
        assertTrue(
            averages.first() >= averages.last(),
            "Expected descending vote_average order, got: $averages",
        )
    }

    @Test
    fun `returns 400 when pageSize exceeds max`() = runWithApp { client ->
        val response = client.get("/movies?pageSize=500")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        val error: ErrorResponse = response.body()
        assertEquals("invalid_page_size", error.error)
    }

    @Test
    fun `returns 400 when year is not a number`() = runWithApp { client ->
        val response = client.get("/movies?year=abc")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        val error: ErrorResponse = response.body()
        assertEquals("invalid_query_param", error.error)
    }

    @Test
    fun `returns 400 when yearFrom greater than yearTo`() = runWithApp { client ->
        val response = client.get("/movies?yearFrom=2020&yearTo=2010")

        assertEquals(HttpStatusCode.BadRequest, response.status)

        val error: ErrorResponse = response.body()
        assertEquals("invalid_year_range", error.error)
    }
}