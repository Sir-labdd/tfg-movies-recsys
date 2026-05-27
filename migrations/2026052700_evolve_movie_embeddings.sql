-- ============================================================================
-- Migration: 2026052700_evolve_movie_embeddings
-- Purpose:   Evolve the movie_embeddings table from its initial draft
--            (created in migration 2026052505 at the end of block B4)
--            to its final shape for block B6.
--
-- Why this migration exists as a separate "evolve" rather than a single
-- "create": the initial table was added at the end of B4 as a placeholder,
-- with only the columns strictly required (movie_id, embedding,
-- generated_at). When B6 actually started populating the table it became
-- clear that two further columns were needed to make the loader
-- idempotent and to leave room for future model migrations. Rather than
-- rewriting history (which would break any environment that already
-- applied 2026052505), the schema is evolved with an additive migration.
-- This is the standard pattern in production database management.
--
-- Changes applied:
--   - ADD model_name TEXT NOT NULL: identifies the model that produced
--     each embedding (currently always 'all-MiniLM-L6-v2', but kept
--     explicit to support future model migrations).
--   - ADD source_text_hash TEXT NOT NULL: SHA-256 of the exact text fed
--     to the model. Lets the loader detect stale embeddings and
--     regenerate only what is needed.
--   - COMMENT ON: documentation comments attached to the table and to
--     each column so the schema is self-describing inside the database.
--
-- The HNSW index is deliberately deferred to migration 2026052701, to be
-- applied AFTER embeddings have been loaded.
-- ============================================================================

ALTER TABLE movie_embeddings
    ADD COLUMN model_name       TEXT,
    ADD COLUMN source_text_hash TEXT;

-- Backfill not needed since the table is empty: simply enforce NOT NULL.
ALTER TABLE movie_embeddings
    ALTER COLUMN model_name       SET NOT NULL,
ALTER COLUMN source_text_hash SET NOT NULL;

COMMENT ON TABLE movie_embeddings IS
    'Semantic embeddings of movie title+overview produced by a
     pretrained sentence-transformers model. One row per movie that
     has a usable synopsis (length >= 30 chars after cleaning).';

COMMENT ON COLUMN movie_embeddings.embedding IS
    '384-dimensional dense vector. Cosine distance is the default
     similarity metric (operator <=>).';

COMMENT ON COLUMN movie_embeddings.model_name IS
    'Identifier of the model that produced the embedding (e.g.
     all-MiniLM-L6-v2). Stored explicitly to support future model
     migrations.';

COMMENT ON COLUMN movie_embeddings.source_text_hash IS
    'SHA-256 of the exact text fed to the model. Used by the loader to
     detect whether a stored embedding is stale and needs regeneration.';

COMMENT ON COLUMN movie_embeddings.generated_at IS
    'Timestamp (with timezone) when this embedding was generated.';