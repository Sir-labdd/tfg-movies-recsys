-- Indexes to speed up GET /movies listing queries.
--
-- The listing endpoint filters by year/language/vote_count and sorts
-- by popularity/vote_average/release_date/title. Without these indexes
-- PostgreSQL would scan all 45,408 movies for every request.
--
-- Some indexes are partial (WHERE col IS NOT NULL) because the listing
-- generally cares only about movies that actually have the field
-- populated. Partial indexes are smaller and faster.

-- Sorting by popularity is the default order. Sparse: 0.1% of rows are
-- NULL after the loading pipeline (P13 cleaning rule), so the partial
-- variant is justified.
CREATE INDEX IF NOT EXISTS idx_movies_popularity_desc
    ON movies (popularity DESC NULLS LAST)
    WHERE popularity IS NOT NULL;

-- Sorting by vote_average; combined with vote_count for quality filters.
-- We index both columns together because the typical query is
-- "ORDER BY vote_average DESC ... WHERE vote_count >= 100".
CREATE INDEX IF NOT EXISTS idx_movies_vote_avg_count
    ON movies (vote_average DESC NULLS LAST, vote_count DESC NULLS LAST)
    WHERE vote_average IS NOT NULL;

-- Sorting / filtering by release_date.
CREATE INDEX IF NOT EXISTS idx_movies_release_date_desc
    ON movies (release_date DESC NULLS LAST)
    WHERE release_date IS NOT NULL;

-- Filtering by original_language (e.g. ?language=en).
CREATE INDEX IF NOT EXISTS idx_movies_original_language
    ON movies (original_language)
    WHERE original_language IS NOT NULL;

-- Filtering by year: a functional index on EXTRACT(YEAR FROM release_date).
-- Lets `WHERE EXTRACT(YEAR FROM release_date) = 2010` hit an index
-- instead of computing the function on every row.
CREATE INDEX IF NOT EXISTS idx_movies_release_year
    ON movies ((EXTRACT(YEAR FROM release_date)::INT))
    WHERE release_date IS NOT NULL;

-- Filtering by minimum vote_count.
CREATE INDEX IF NOT EXISTS idx_movies_vote_count
    ON movies (vote_count)
    WHERE vote_count IS NOT NULL;

-- For the genre filter, we already have idx on movie_genres(movie_id)
-- via the PK. We add the reverse direction so "find movies of genre X"
-- is also indexed.
CREATE INDEX IF NOT EXISTS idx_movie_genres_genre_id
    ON movie_genres (genre_id);

COMMENT ON INDEX idx_movies_popularity_desc IS
    'Default sort order for GET /movies listings';
COMMENT ON INDEX idx_movies_vote_avg_count IS
    'Sort by vote_average with vote_count tiebreaker; ratings ranking';
COMMENT ON INDEX idx_movies_release_year IS
    'Functional index for year filter (EXTRACT(YEAR FROM release_date))';