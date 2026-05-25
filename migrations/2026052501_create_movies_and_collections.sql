-- ============================================================================
-- Migration: 2026052501_create_movies_and_collections
-- Purpose:   Create the central 'movies' table and the auxiliary
--            'collections' table for film sagas/franchises.
--
-- Design notes:
--   - movies.id is the TMDB id, used directly as primary key. No
--     surrogate auto-increment because relations between source files
--     are already established using TMDB ids.
--   - All non-essential columns are nullable; the cleaning pipeline
--     guarantees the data is internally consistent (zeros to NaN,
--     placeholder text to NaN, etc).
--   - collections is a separate table because (a) a collection has
--     its own metadata (name, posters) and (b) many movies can share
--     the same collection (1-to-many relationship from collections to
--     movies).
--   - Indexes are created on columns expected to be filtered or
--     sorted often in the API (release_date for temporal filters,
--     popularity for ranked listings).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- collections: film sagas / franchises
-- ----------------------------------------------------------------------------
CREATE TABLE collections (
                             id              INTEGER     PRIMARY KEY,
                             name            TEXT        NOT NULL,
                             poster_path     TEXT        NULL,
                             backdrop_path   TEXT        NULL
);

COMMENT ON TABLE collections IS
    'Movie sagas/franchises (e.g. "Toy Story Collection"). A movie may
     belong to zero or one collection.';

-- ----------------------------------------------------------------------------
-- movies: central entity
-- ----------------------------------------------------------------------------
CREATE TABLE movies (
                        id                  INTEGER         PRIMARY KEY,
                        imdb_id             VARCHAR(15)     NULL,
                        title               TEXT            NOT NULL,
                        original_title      TEXT            NULL,
                        original_language   VARCHAR(10)     NULL,
                        overview            TEXT            NULL,
                        tagline             TEXT            NULL,
                        release_date        DATE            NULL,
                        runtime             INTEGER         NULL,
                        budget              BIGINT          NULL,
                        revenue             BIGINT          NULL,
                        popularity          NUMERIC(8, 3)   NULL,
                        vote_average        NUMERIC(3, 1)   NULL,
                        vote_count          INTEGER         NULL,
                        status              VARCHAR(30)     NOT NULL DEFAULT 'Unknown',
                        adult               BOOLEAN         NOT NULL DEFAULT FALSE,
                        poster_path         TEXT            NULL,
                        collection_id       INTEGER         NULL REFERENCES collections(id)
                                            ON DELETE SET NULL
);

COMMENT ON TABLE movies IS
    'Central movie entity. One row per film. Source: TMDB via The
     Movies Dataset on Kaggle.';

COMMENT ON COLUMN movies.id IS 'TMDB id used as primary key.';
COMMENT ON COLUMN movies.budget IS 'Budget in USD. NULL when not reported.';
COMMENT ON COLUMN movies.revenue IS 'Box office revenue in USD. NULL when not reported.';
COMMENT ON COLUMN movies.vote_average IS 'Mean rating on TMDB (0-10). NULL when vote_count is 0.';

-- ----------------------------------------------------------------------------
-- Indexes
-- ----------------------------------------------------------------------------
CREATE INDEX idx_movies_release_date ON movies (release_date)
    WHERE release_date IS NOT NULL;

CREATE INDEX idx_movies_popularity_desc ON movies (popularity DESC NULLS LAST)
    WHERE popularity IS NOT NULL;

CREATE INDEX idx_movies_vote_average ON movies (vote_average DESC NULLS LAST)
    WHERE vote_average IS NOT NULL;

CREATE INDEX idx_movies_original_language ON movies (original_language);

CREATE INDEX idx_movies_collection_id ON movies (collection_id)
    WHERE collection_id IS NOT NULL;

-- Index for case-insensitive, accent-insensitive title search.
-- Built on the unaccent extension installed in migration 2026052500.
CREATE INDEX idx_movies_title_lower_unaccent
    ON movies (lower(unaccent(title)));