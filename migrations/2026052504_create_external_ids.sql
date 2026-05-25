-- ============================================================================
-- Migration: 2026052504_create_external_ids
-- Purpose:   Create the table mapping movies to external identifiers
--            (MovieLens id, IMDb id from links.csv).
--
-- Design notes:
--   - movie_id is the FK to movies (TMDB id) and serves as primary key:
--     a movie has at most one row of external identifiers.
--   - movielens_id and imdb_id_full are nullable because the dataset
--     does not always provide both. They are UNIQUE so a given external
--     id maps to a single TMDB id.
--   - The 'imdb_id' field in movies.imdb_id (e.g. 'tt0114709') is kept
--     there for backward compatibility with the CSV. This table can
--     hold the imdb_id again to follow the canonical link structure;
--     if it duplicates the value in movies, we'll address it during
--     the data loading block.
-- ============================================================================

CREATE TABLE external_ids (
                              movie_id        INTEGER     PRIMARY KEY REFERENCES movies(id) ON DELETE CASCADE,
                              movielens_id    INTEGER     NULL UNIQUE,
                              imdb_id_full    VARCHAR(15) NULL UNIQUE
);

COMMENT ON TABLE external_ids IS
    'Mapping from TMDB ids (canonical for this project) to external
     ids in other movie databases (MovieLens, IMDb). Source: links.csv.
     Designed to allow future cross-referencing with external rating
     datasets if collaborative filtering is added.';

COMMENT ON COLUMN external_ids.movielens_id IS
    'Movie identifier in the MovieLens dataset.';

COMMENT ON COLUMN external_ids.imdb_id_full IS
    'IMDb identifier (e.g. tt0114709). Same value lives in movies.imdb_id
     for direct access; kept here too to preserve the canonical structure
     of links.csv.';

CREATE INDEX idx_external_ids_movielens ON external_ids (movielens_id)
    WHERE movielens_id IS NOT NULL;