-- ============================================================================
-- Migration: 2026052500_init_extensions
-- Purpose:   Enable required PostgreSQL extensions.
--
-- Extensions installed:
--   - vector (pgvector): adds the 'vector' data type and similarity
--     operators required by the embeddings-based recommender.
--   - unaccent: removes accents during text search, enabling case- and
--     accent-insensitive title search ("amelie" matches "Amélie").
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS unaccent;