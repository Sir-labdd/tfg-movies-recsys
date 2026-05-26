package com.tfg.movies.server.movies

import com.tfg.movies.server.config.DatabaseFactory
import com.tfg.movies.shared.movies.CastMember
import com.tfg.movies.shared.movies.CollectionRef
import com.tfg.movies.shared.movies.CrewMember
import com.tfg.movies.shared.movies.CrossReferences
import com.tfg.movies.shared.movies.MovieDetails
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.sql.ResultSet

/**
 * Repository for movies. Translates between the SQL view v_movie_details
 * and the MovieDetails DTO (defined in :shared). This class contains
 * no business logic — only data access.
 */
class MovieRepository {

    private val logger = LoggerFactory.getLogger(MovieRepository::class.java)

    /**
     * Fetches a movie by its TMDB id. Returns null if the movie does
     * not exist.
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

    /**
     * Maps a row of v_movie_details to a MovieDetails DTO.
     */
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