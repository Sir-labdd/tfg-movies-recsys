-- ============================================================================
-- Migration: 2026052505_create_embeddings
-- Purpose:   Create the table storing semantic embeddings for movie
--            synopses, used by the content-based recommender.
--
-- Design notes:
--   - One embedding per movie, identified by movie_id (PK + FK).
--   - Vector dimension is fixed at 384 (matches sentence-transformers
--     'all-MiniLM-L6-v2' model output).
--   - generated_at allows tracking when each embedding was computed,
--     useful if the model changes and embeddings need regeneration.
--   - Not all movies will have an embedding: those with missing or
--     too-short overviews will be excluded during the generation
--     phase. This is enforced at the application layer, not at the
--     database layer.
--   - An HNSW (Hierarchical Navigable Small World) index is created
--     for fast approximate nearest-neighbor search. The vector_cosine_ops
--     operator class is used because cosine similarity is the standard
--     metric for sentence embeddings.
-- ============================================================================

CREATE TABLE movie_embeddings (
                                  movie_id        INTEGER         PRIMARY KEY REFERENCES movies(id) ON DELETE CASCADE,
                                  embedding       VECTOR(384)     NOT NULL,
                                  generated_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE movie_embeddings IS
    'Semantic embeddings of movie synopses (overview field), generated
     using sentence-transformers all-MiniLM-L6-v2. Used by the content-
     based recommender for similarity search.';

COMMENT ON COLUMN movie_embeddings.embedding IS
    '384-dimensional vector representing the semantic content of the
     synopsis. Normalized to unit length for cosine similarity.';

COMMENT ON COLUMN movie_embeddings.generated_at IS
    'Timestamp of when this embedding was computed. Used to detect
     stale embeddings if the underlying model changes.';

-- ----------------------------------------------------------------------------
-- HNSW index for approximate nearest-neighbor search by cosine distance.
--
-- HNSW (Hierarchical Navigable Small World) provides excellent query
-- performance for similarity search at the cost of slower index build
-- time. The trade-off is acceptable here because embeddings are computed
-- once offline and queried many times.
--
-- The 'vector_cosine_ops' operator class enables the '<=>' cosine
-- distance operator, which we'll use in recommendation queries.
--
-- Parameters m and ef_construction follow the pgvector defaults
-- (16 and 64 respectively), which provide a good balance between
-- index size, build time and recall for our dataset size.
-- ----------------------------------------------------------------------------
CREATE INDEX idx_movie_embeddings_hnsw
    ON movie_embeddings
    USING hnsw (embedding vector_cosine_ops);