-- ============================================================================
-- Migration: 2026052703_recreate_hnsw_index_768
-- Purpose:   Recreate the HNSW index after the embedding column was
--            resized from vector(384) to vector(768) in migration 2026052702.
--
-- When migration 2026052702 dropped the embedding column to recreate
-- it with a different dimension, the HNSW index that depended on it
-- was automatically dropped by PostgreSQL (the index could not exist
-- without its column). This migration restores it on the new column.
--
-- Parameters are the same as the original HNSW index in 2026052701
-- (m = 16, ef_construction = 64), recommended by the pgvector docs
-- for datasets of this order of magnitude. The index is built over
-- the now-populated table (44k+ embeddings), which is exactly when
-- HNSW gives its best build-time-to-recall ratio.
--
-- Index name includes the suffix _768 to make the dimension explicit
-- in the schema, helping future maintenance.
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_movie_embeddings_hnsw_cosine_768
    ON movie_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

COMMENT ON INDEX idx_movie_embeddings_hnsw_cosine_768 IS
    'HNSW index for cosine similarity on 768-dim embeddings produced by
     all-mpnet-base-v2. Replaces the previous 384-dim index dropped by
     migration 2026052702 when the embedding column was resized.';