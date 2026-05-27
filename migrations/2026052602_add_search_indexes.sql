-- Functional indexes for movie search.
--
-- The search endpoint normalizes hyphens to spaces so that
-- "spider man" matches "Spider-Man" and vice versa. The
-- normalization must be applied identically to the column and
-- the query, so we create a functional index that mirrors the
-- expression used in the WHERE clause:
--
--   replace(lower(immutable_unaccent(title)), '-', ' ')
--
-- Without this index, every search would scan all 45,408 movies.

CREATE INDEX IF NOT EXISTS idx_movies_search_title
    ON movies (
    (replace(lower(immutable_unaccent(title)), '-', ' '))
    );

CREATE INDEX IF NOT EXISTS idx_movies_search_original_title
    ON movies (
    (replace(lower(immutable_unaccent(original_title)), '-', ' '))
    )
    WHERE original_title IS NOT NULL;

COMMENT ON INDEX idx_movies_search_title IS
    'Functional index matching the normalization used by the /movies/search endpoint';
COMMENT ON INDEX idx_movies_search_original_title IS
    'Functional index on original_title for cross-title search support';