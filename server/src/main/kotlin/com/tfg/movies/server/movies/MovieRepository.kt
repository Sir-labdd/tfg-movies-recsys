package com.tfg.movies.server.movies

import com.tfg.movies.server.config.DatabaseFactory
import com.tfg.movies.shared.movies.CastMember
import com.tfg.movies.shared.movies.CollectionRef
import com.tfg.movies.shared.movies.CrewMember
import com.tfg.movies.shared.movies.CrossReferences
import com.tfg.movies.shared.movies.MovieDetails
import com.tfg.movies.shared.movies.MovieSortBy
import com.tfg.movies.shared.movies.MovieSummary
import com.tfg.movies.shared.movies.SortDirection
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.sql.ResultSet

/**
 * Repository for movies. Translates between the database and the
 * MovieDetails / MovieSummary DTOs (defined in :shared). This class
 * contains no business logic — only data access.
 */
class MovieRepository {

    private val logger = LoggerFactory.getLogger(MovieRepository::class.java)

    // ---------- single movie ----------

    /**
     * Fetches a movie by its TMDB id. Returns null if it does not exist.
     */
    fun findById(id: Int): MovieDetails? {
        val sql = "SELECT * FROM v_movie_details WHERE id = ?"

        DatabaseFactory.dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) mapRowToMovieDetails(rs) else null
                }
            }
        }
    }

    // ---------- listings ----------

    /**
     * Filters accepted by [findAll]. Null fields mean "no filter for
     * this dimension". Combined with AND.
     */
    data class ListFilters(
        val genres: List<String> = emptyList(), // OR semantics if multiple
        val year: Int? = null,                   // exact year
        val yearFrom: Int? = null,               // range start (inclusive)
        val yearTo: Int? = null,                 // range end (inclusive)
        val language: String? = null,            // ISO 639-1 code
        val minVoteCount: Int? = null,
    )

    /**
     * Sort specification for [findAll].
     */
    data class SortSpec(
        val by: MovieSortBy = MovieSortBy.POPULARITY,
        val direction: SortDirection = SortDirection.DESC,
    )

    /**
     * Pagination specification for [findAll].
     */
    data class Pagination(
        val page: Int,
        val pageSize: Int,
    ) {
        val offset: Int get() = (page - 1) * pageSize
    }

    /**
     * Result of a paginated query: the page slice plus the total count
     * across the filtered universe (not just the current page).
     */
    data class PagedResult(
        val items: List<MovieSummary>,
        val total: Long,
    )

    /**
     * Returns a page of movies matching the given filters, sorted as
     * specified. Returns both the items and the total count so the
     * caller can build the paginated response.
     *
     * The SQL is built dynamically. Filters not provided are skipped.
     * All values are bound via prepared statement parameters; no string
     * concatenation of user input into the SQL.
     */
    fun findAll(
        filters: ListFilters,
        sort: SortSpec,
        pagination: Pagination,
    ): PagedResult {
        val whereBuilder = WhereBuilder()
        applyFilters(whereBuilder, filters)
        val whereClause = whereBuilder.toClause()
        val params = whereBuilder.params

        // Main query: page of items
        val orderBy = buildOrderBy(sort)
        val listSql = """
            SELECT
                m.id,
                m.title,
                m.poster_path,
                m.popularity,
                m.vote_average,
                m.vote_count,
                EXTRACT(YEAR FROM m.release_date)::INT AS year,
                COALESCE(
                    (SELECT ARRAY_AGG(g.name ORDER BY g.name)
                     FROM movie_genres mg
                     JOIN genres g ON g.id = mg.genre_id
                     WHERE mg.movie_id = m.id),
                    ARRAY[]::TEXT[]
                ) AS genres
            FROM movies m
            $whereClause
            ORDER BY $orderBy
            LIMIT ? OFFSET ?
        """.trimIndent()

        // Count query: same filters, no ORDER/LIMIT/OFFSET
        val countSql = """
            SELECT COUNT(*) AS total
            FROM movies m
            $whereClause
        """.trimIndent()

        DatabaseFactory.dataSource.connection.use { conn ->
            // 1. Fetch total
            val total = conn.prepareStatement(countSql).use { stmt ->
                bindParams(stmt, params)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getLong("total") else 0L
                }
            }

            // 2. Fetch items
            val items = conn.prepareStatement(listSql).use { stmt ->
                bindParams(stmt, params)
                // LIMIT and OFFSET as the last two parameters
                stmt.setInt(params.size + 1, pagination.pageSize)
                stmt.setInt(params.size + 2, pagination.offset)
                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<MovieSummary>()
                    while (rs.next()) {
                        results.add(mapRowToMovieSummary(rs))
                    }
                    results
                }
            }

            return PagedResult(items = items, total = total)
        }
    }

    fun searchByTitle(
        query: String,
        pagination: Pagination,
    ): PagedResult {
        // Same pattern transformations as before for the LIKE wildcards
        val pattern = "%$query%"
        val patternStart = "$query%"

        // Helper SQL fragment applied to both the column and the query
        // value. Lowercases, removes accents, and treats hyphens as
        // spaces so "spider man" matches "Spider-Man".
        val titleNorm = "replace(lower(immutable_unaccent(m.title)), '-', ' ')"
        val origNorm  = "replace(lower(immutable_unaccent(m.original_title)), '-', ' ')"
        val queryNorm = "replace(lower(immutable_unaccent(?)), '-', ' ')"

        val listSql = """
            SELECT
                m.id,
                m.title,
                m.poster_path,
                m.popularity,
                m.vote_average,
                m.vote_count,
                EXTRACT(YEAR FROM m.release_date)::INT AS year,
                COALESCE(
                    (SELECT ARRAY_AGG(g.name ORDER BY g.name)
                     FROM movie_genres mg
                     JOIN genres g ON g.id = mg.genre_id
                     WHERE mg.movie_id = m.id),
                    ARRAY[]::TEXT[]
                ) AS genres,
                CASE
                    WHEN $titleNorm = $queryNorm THEN 100
                    WHEN $titleNorm LIKE $queryNorm THEN 50
                    WHEN $titleNorm LIKE $queryNorm THEN 10
                    WHEN $origNorm  LIKE $queryNorm THEN 5
                    ELSE 0
                END AS relevance_score
            FROM movies m
            WHERE
                $titleNorm LIKE $queryNorm
                OR $origNorm LIKE $queryNorm
            ORDER BY relevance_score DESC, m.popularity DESC NULLS LAST, m.id ASC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val countSql = """
            SELECT COUNT(*) AS total
            FROM movies m
            WHERE
                $titleNorm LIKE $queryNorm
                OR $origNorm LIKE $queryNorm
        """.trimIndent()

        DatabaseFactory.dataSource.connection.use { conn ->
            // Count: 2 query params (one per WHERE clause)
            val total = conn.prepareStatement(countSql).use { stmt ->
                stmt.setString(1, pattern)
                stmt.setString(2, pattern)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getLong("total") else 0L
                }
            }

            // List: 6 query params for the SELECT (4 in CASE + 2 in WHERE)
            //       + 2 for LIMIT/OFFSET
            // Param positions:
            //   1: exact match (CASE)        -> query (no wildcards)
            //   2: starts-with (CASE)        -> patternStart
            //   3: title contains (CASE)     -> pattern
            //   4: original contains (CASE)  -> pattern
            //   5: WHERE title               -> pattern
            //   6: WHERE original            -> pattern
            //   7: LIMIT
            //   8: OFFSET
            val items = conn.prepareStatement(listSql).use { stmt ->
                stmt.setString(1, query)
                stmt.setString(2, patternStart)
                stmt.setString(3, pattern)
                stmt.setString(4, pattern)
                stmt.setString(5, pattern)
                stmt.setString(6, pattern)
                stmt.setInt(7, pagination.pageSize)
                stmt.setInt(8, pagination.offset)
                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<MovieSummary>()
                    while (rs.next()) {
                        results.add(mapRowToMovieSummary(rs))
                    }
                    results
                }
            }

            return PagedResult(items = items, total = total)
        }
    }

    // ---------- private helpers: WHERE/ORDER BY/binding ----------

    /**
     * Mutable accumulator for WHERE clauses and their bound parameters,
     * in the order the placeholders appear in the final SQL.
     */
    private class WhereBuilder {
        private val clauses = mutableListOf<String>()
        val params = mutableListOf<Any>()

        fun add(clause: String, vararg values: Any) {
            clauses.add(clause)
            params.addAll(values)
        }

        fun toClause(): String =
            if (clauses.isEmpty()) "" else "WHERE " + clauses.joinToString(" AND ")
    }

    private fun applyFilters(b: WhereBuilder, f: ListFilters) {
        if (f.genres.isNotEmpty()) {
            // EXISTS with IN: "movie has at least one of the requested genres"
            val placeholders = f.genres.joinToString(",") { "?" }
            b.add(
                """
                EXISTS (
                    SELECT 1 FROM movie_genres mg
                    JOIN genres g ON g.id = mg.genre_id
                    WHERE mg.movie_id = m.id AND g.name IN ($placeholders)
                )
                """.trimIndent(),
                *f.genres.toTypedArray()
            )
        }
        if (f.year != null) {
            b.add("EXTRACT(YEAR FROM m.release_date)::INT = ?", f.year)
        }
        if (f.yearFrom != null) {
            b.add("EXTRACT(YEAR FROM m.release_date)::INT >= ?", f.yearFrom)
        }
        if (f.yearTo != null) {
            b.add("EXTRACT(YEAR FROM m.release_date)::INT <= ?", f.yearTo)
        }
        if (f.language != null) {
            b.add("m.original_language = ?", f.language)
        }
        if (f.minVoteCount != null) {
            b.add("m.vote_count >= ?", f.minVoteCount)
        }
    }

    /**
     * Translates a SortSpec into a SQL ORDER BY clause. Column names
     * are hard-coded — not user input — so they cannot cause injection.
     */
    private fun buildOrderBy(sort: SortSpec): String {
        val column = when (sort.by) {
            MovieSortBy.POPULARITY    -> "m.popularity"
            MovieSortBy.VOTE_AVERAGE  -> "m.vote_average"
            MovieSortBy.VOTE_COUNT    -> "m.vote_count"
            MovieSortBy.RELEASE_DATE  -> "m.release_date"
            MovieSortBy.TITLE         -> "m.title"
        }
        val direction = when (sort.direction) {
            SortDirection.ASC  -> "ASC"
            SortDirection.DESC -> "DESC"
        }
        // NULLS LAST so missing data does not float to the top of the page.
        return "$column $direction NULLS LAST, m.id ASC"
    }

    /**
     * Binds the accumulated WHERE parameters to a prepared statement,
     * 1-indexed as JDBC requires.
     */
    private fun bindParams(stmt: java.sql.PreparedStatement, params: List<Any>) {
        params.forEachIndexed { i, value ->
            stmt.setObject(i + 1, value)
        }
    }

    // ---------- private helpers: row → DTO ----------

    private fun mapRowToMovieSummary(rs: ResultSet): MovieSummary {
        return MovieSummary(
            id = rs.getInt("id"),
            title = rs.getString("title"),
            year = rs.getObject("year") as Int?,
            posterPath = rs.getString("poster_path"),
            voteAverage = rs.getObject("vote_average")?.let { (it as java.math.BigDecimal).toDouble() },
            voteCount = rs.getObject("vote_count") as Int?,
            popularity = rs.getObject("popularity")?.let { (it as java.math.BigDecimal).toDouble() },
            genres = readStringArray(rs, "genres"),
        )
    }

    private fun mapRowToMovieDetails(rs: ResultSet): MovieDetails {
        val collectionId = rs.getInt("collection_id")
        val collection = if (rs.wasNull()) null else CollectionRef(
            id = collectionId,
            name = rs.getString("collection_name"),
            posterPath = rs.getString("collection_poster_path"),
        )

        val movielensIdRaw = rs.getInt("movielens_id")
        val movielensId = if (rs.wasNull()) null else movielensIdRaw

        return MovieDetails(
            id = rs.getInt("id"),
            title = rs.getString("title"),
            originalTitle = rs.getString("original_title"),
            originalLanguage = rs.getString("original_language"),
            overview = rs.getString("overview"),
            tagline = rs.getString("tagline"),
            releaseDate = rs.getDate("release_date")?.toString(),
            runtime = rs.getObject("runtime") as Int?,
            status = rs.getString("status"),
            budget = rs.getObject("budget") as Long?,
            revenue = rs.getObject("revenue") as Long?,
            popularity = rs.getObject("popularity")?.let { (it as java.math.BigDecimal).toDouble() },
            voteAverage = rs.getObject("vote_average")?.let { (it as java.math.BigDecimal).toDouble() },
            voteCount = rs.getObject("vote_count") as Int?,
            adult = rs.getBoolean("adult"),
            posterPath = rs.getString("poster_path"),
            imdbId = rs.getString("imdb_id"),
            collection = collection,
            genres = readStringArray(rs, "genres"),
            productionCountries = readStringArray(rs, "production_countries"),
            spokenLanguages = readStringArray(rs, "spoken_languages"),
            productionCompanies = readStringArray(rs, "production_companies"),
            keywords = readStringArray(rs, "keywords"),
            cast = parseJsonArray<CastMember>(rs.getString("cast_json")),
            crew = parseJsonArray<CrewMember>(rs.getString("crew_json")),
            crossReferences = CrossReferences(
                movielensId = movielensId,
                imdbNumeric = rs.getString("imdb_id_full"),
            ),
        )
    }

    // ---------- private helpers: arrays/JSON ----------

    private fun readStringArray(rs: ResultSet, columnName: String): List<String> {
        val sqlArray = rs.getArray(columnName) ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        return (sqlArray.array as Array<String>).toList()
    }

    private inline fun <reified T> parseJsonArray(json: String?): List<T> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        return JsonParser.decodeFromString(json)
    }

    companion object {
        private val JsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}