-- ============================================================================
-- Migration: 2026052503_create_categories
-- Purpose:   Create tables for movie categorization (genres, production
--            companies, production countries, spoken languages, keywords)
--            and their many-to-many relations with movies.
--
-- Design notes:
--   - All entities follow the same pattern: a canonical table with id and
--     name, and a junction table connecting movies to multiple entries.
--   - Countries and languages use ISO codes as primary keys instead of
--     surrogate ids. ISO codes (us, es, fr, en, etc) are stable,
--     universal, and human-readable, which is the standard practice for
--     international datasets.
--   - Junction tables use composite primary keys.
--   - ON DELETE CASCADE so that removing a movie cleans up its junctions.
--   - Canonical entity tables do NOT cascade from movie deletion; if
--     all movies of a genre disappear, the genre itself stays in the
--     catalog.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- genres
-- ----------------------------------------------------------------------------
CREATE TABLE genres (
                        id      INTEGER     PRIMARY KEY,
                        name    TEXT        NOT NULL UNIQUE
);

COMMENT ON TABLE genres IS 'Canonical list of film genres (Drama, Comedy, etc).';

CREATE TABLE movie_genres (
                              movie_id    INTEGER NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                              genre_id    INTEGER NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
                              PRIMARY KEY (movie_id, genre_id)
);

CREATE INDEX idx_movie_genres_genre ON movie_genres (genre_id);

-- ----------------------------------------------------------------------------
-- production_companies
-- ----------------------------------------------------------------------------
CREATE TABLE production_companies (
                                      id      INTEGER     PRIMARY KEY,
                                      name    TEXT        NOT NULL
);

COMMENT ON TABLE production_companies IS
    'Production companies (Warner Bros, A24, etc). Multiple movies share
     the same company; one movie can be produced by several companies.';

CREATE INDEX idx_production_companies_name ON production_companies (lower(name));

CREATE TABLE movie_production_companies (
                                            movie_id    INTEGER NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                            company_id  INTEGER NOT NULL REFERENCES production_companies(id) ON DELETE CASCADE,
                                            PRIMARY KEY (movie_id, company_id)
);

CREATE INDEX idx_movie_production_companies_company
    ON movie_production_companies (company_id);

-- ----------------------------------------------------------------------------
-- production_countries
-- ----------------------------------------------------------------------------
CREATE TABLE production_countries (
                                      iso_3166_1  VARCHAR(2)  PRIMARY KEY,
                                      name        TEXT        NOT NULL
);

COMMENT ON TABLE production_countries IS
    'Production countries identified by their ISO 3166-1 alpha-2 code
     (us, es, fr, etc). Code used as primary key for stability and
     human-readability.';

CREATE TABLE movie_production_countries (
                                            movie_id        INTEGER     NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                            country_iso     VARCHAR(2)  NOT NULL REFERENCES production_countries(iso_3166_1)
                                                ON DELETE CASCADE,
                                            PRIMARY KEY (movie_id, country_iso)
);

CREATE INDEX idx_movie_production_countries_country
    ON movie_production_countries (country_iso);

-- ----------------------------------------------------------------------------
-- spoken_languages
-- ----------------------------------------------------------------------------
CREATE TABLE spoken_languages (
                                  iso_639_1   VARCHAR(2)  PRIMARY KEY,
                                  name        TEXT        NOT NULL
);

COMMENT ON TABLE spoken_languages IS
    'Languages spoken in films, identified by ISO 639-1 two-letter code
     (en, es, fr, etc). Code used as primary key.';

CREATE TABLE movie_spoken_languages (
                                        movie_id        INTEGER     NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                        language_iso    VARCHAR(2)  NOT NULL REFERENCES spoken_languages(iso_639_1)
                                            ON DELETE CASCADE,
                                        PRIMARY KEY (movie_id, language_iso)
);

CREATE INDEX idx_movie_spoken_languages_lang
    ON movie_spoken_languages (language_iso);

-- ----------------------------------------------------------------------------
-- keywords
-- ----------------------------------------------------------------------------
CREATE TABLE keywords (
                          id      INTEGER     PRIMARY KEY,
                          name    TEXT        NOT NULL UNIQUE
);

COMMENT ON TABLE keywords IS
    'Thematic keywords associated with movies (e.g. "time travel",
     "post-apocalyptic", "based on novel"). Source: TMDB keywords API.';

CREATE INDEX idx_keywords_name ON keywords (lower(name));

CREATE TABLE movie_keywords (
                                movie_id    INTEGER NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                keyword_id  INTEGER NOT NULL REFERENCES keywords(id) ON DELETE CASCADE,
                                PRIMARY KEY (movie_id, keyword_id)
);

CREATE INDEX idx_movie_keywords_keyword ON movie_keywords (keyword_id);