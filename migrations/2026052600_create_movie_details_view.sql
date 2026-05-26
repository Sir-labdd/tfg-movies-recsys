-- View v_movie_details
--
-- Aggregates all read-only data needed to describe a movie in a single
-- row. Used by GET /movies/{id} so the backend issues exactly one
-- query instead of one per relation (genres, countries, cast, etc.).
--
-- JSON_AGG / ARRAY_AGG collapse N-to-M relations into arrays/JSON
-- embedded in the row. Each relation is gathered via a correlated
-- scalar subquery (instead of a single mega-JOIN with GROUP BY) to
-- avoid the combinatorial blow-up of cartesian intermediate rows.

CREATE OR REPLACE VIEW v_movie_details AS
SELECT
    m.id,
    m.imdb_id,
    m.title,
    m.original_title,
    m.original_language,
    m.overview,
    m.tagline,
    m.release_date,
    m.runtime,
    m.status,
    m.budget,
    m.revenue,
    m.popularity,
    m.vote_average,
    m.vote_count,
    m.adult,
    m.poster_path,

    -- Collection (1-N: a movie has at most 1 collection)
    c.id           AS collection_id,
    c.name         AS collection_name,
    c.poster_path  AS collection_poster_path,

    -- External IDs (1-1)
    ext.movielens_id,
    ext.imdb_id_full,

    -- Genres (N-M): array of names sorted alphabetically
    COALESCE(
            (SELECT ARRAY_AGG(g.name ORDER BY g.name)
             FROM movie_genres mg
                      JOIN genres g ON g.id = mg.genre_id
             WHERE mg.movie_id = m.id),
            ARRAY[]::TEXT[]
    ) AS genres,

    -- Production countries (N-M)
    COALESCE(
            (SELECT ARRAY_AGG(pc_co.name ORDER BY pc_co.name)
             FROM movie_production_countries mpc
                      JOIN production_countries pc_co ON pc_co.iso_3166_1 = mpc.country_iso
             WHERE mpc.movie_id = m.id),
            ARRAY[]::TEXT[]
    ) AS production_countries,

    -- Spoken languages (N-M)
    COALESCE(
            (SELECT ARRAY_AGG(sl.name ORDER BY sl.name)
             FROM movie_spoken_languages msl
                      JOIN spoken_languages sl ON sl.iso_639_1 = msl.language_iso
             WHERE msl.movie_id = m.id),
            ARRAY[]::TEXT[]
    ) AS spoken_languages,

    -- Production companies (N-M)
    COALESCE(
            (SELECT ARRAY_AGG(pc.name ORDER BY pc.name)
             FROM movie_production_companies mpcomp
                      JOIN production_companies pc ON pc.id = mpcomp.company_id
             WHERE mpcomp.movie_id = m.id),
            ARRAY[]::TEXT[]
    ) AS production_companies,

    -- Keywords (N-M)
    COALESCE(
            (SELECT ARRAY_AGG(k.name ORDER BY k.name)
             FROM movie_keywords mk
                      JOIN keywords k ON k.id = mk.keyword_id
             WHERE mk.movie_id = m.id),
            ARRAY[]::TEXT[]
    ) AS keywords,

    -- Cast (N-M): JSON array of objects, sorted by cast_order
    COALESCE(
            (SELECT JSON_AGG(
                            JSON_BUILD_OBJECT(
                                    'personId', p.id,
                                    'name', p.name,
                                    'character', mc.character_name,
                                    'order', mc.cast_order
                            ) ORDER BY mc.cast_order
                    )
             FROM movie_cast mc
                      JOIN people p ON p.id = mc.person_id
             WHERE mc.movie_id = m.id),
            '[]'::JSON
    ) AS cast_json,

    -- Crew (N-M): JSON array of objects, sorted by department then job
    COALESCE(
            (SELECT JSON_AGG(
                            JSON_BUILD_OBJECT(
                                    'personId', p.id,
                                    'name', p.name,
                                    'job', mcr.job,
                                    'department', mcr.department
                            ) ORDER BY mcr.department, mcr.job
                    )
             FROM movie_crew mcr
                      JOIN people p ON p.id = mcr.person_id
             WHERE mcr.movie_id = m.id),
            '[]'::JSON
    ) AS crew_json

FROM movies m
         LEFT JOIN collections   c   ON c.id = m.collection_id
         LEFT JOIN external_ids  ext ON ext.movie_id = m.id;

COMMENT ON VIEW v_movie_details IS
    'Denormalized read view aggregating a movie with all its N-to-M relations '
        'as PostgreSQL arrays and JSON. Designed for GET /movies/{id}.';