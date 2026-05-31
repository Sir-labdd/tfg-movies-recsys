"""
Generates semantic embeddings for movies and stores them in PostgreSQL.

Reads:  movies table (id, title, overview).
Writes: movie_embeddings table.

Pipeline:
1. Connect to PostgreSQL and register the pgvector adapter so that
   NumPy arrays can be inserted directly into the 'vector(768)' column.
2. Load the all-mpnet-base-v2 sentence-transformers model. First run
   downloads it from HuggingFace (~420 MB) into the local cache; later
   runs read from cache.
3. Fetch the candidate movies: those with overview length >= 30 chars.
   The cleaning pipeline already replaced placeholder synopses with
   NULL, so this filter excludes those automatically.
4. For each candidate, build the source text as 'title + overview',
   compute its SHA-256 hash, and compare with the hash already stored
   in movie_embeddings (if any). Skip the movie if the hash matches:
   nothing has changed since the last run.
5. Encode the remaining texts in batches of 64 and upsert the rows
   into movie_embeddings with INSERT ... ON CONFLICT DO UPDATE.
6. Report counts and timing.

Usage:
    uv run python python/scripts/13_generate_embeddings.py
    uv run python python/scripts/13_generate_embeddings.py --limit 1000
    uv run python python/scripts/13_generate_embeddings.py --force
"""

import argparse
import hashlib
import logging
import os
import sys
import time
from pathlib import Path
from typing import Iterator

import numpy as np
import psycopg
from pgvector.psycopg import register_vector
from sentence_transformers import SentenceTransformer
from tqdm import tqdm


# ----------------------------------------------------------------------------
# Environment loading
# ----------------------------------------------------------------------------

PROJECT_ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = PROJECT_ROOT / ".env"


def load_env_file(path: Path) -> dict[str, str]:
    """
    Parse a .env file into a dict. Minimal implementation matching the
    style of 07_migrate.py: ignores comments and empty lines, splits on
    the first '='. Keeping the same parser across scripts avoids adding
    python-dotenv as a dependency for such a small need.
    """
    env: dict[str, str] = {}
    if not path.exists():
        return env
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        key, _, value = line.partition("=")
        env[key.strip()] = value.strip()
    return env
# ----------------------------------------------------------------------------
# Configuration
# ----------------------------------------------------------------------------

MODEL_NAME = "sentence-transformers/all-mpnet-base-v2"
EMBEDDING_DIM = 768

# Identifier stored in movie_embeddings.model_name. Kept short and
# stable so future model migrations can be tracked by string comparison.
MODEL_TAG = "all-mpnet-base-v2"

# Batch sizes:
# - ENCODE_BATCH: passed to model.encode(). Larger = faster but more RAM.
# - DB_BATCH: number of rows per upsert. Larger = fewer roundtrips but
#   bigger SQL statements.
ENCODE_BATCH = 64
DB_BATCH = 256

# Minimum overview length to consider the movie a candidate. The cleaning
# pipeline (P15 in section 7.2.5 of the memoir) flagged short overviews
# as too noisy for embeddings. This filter mirrors that decision.
MIN_OVERVIEW_LENGTH = 30


# ----------------------------------------------------------------------------
# Logging
# ----------------------------------------------------------------------------

def setup_logging() -> logging.Logger:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s | %(levelname)-7s | %(message)s",
        datefmt="%H:%M:%S",
    )
    return logging.getLogger("embeddings")


# ----------------------------------------------------------------------------
# Database helpers
# ----------------------------------------------------------------------------

def open_connection() -> psycopg.Connection:
    """
    Open a PostgreSQL connection and register the pgvector adapter.

    DATABASE_URL is resolved in this order:
      1. The current process environment (if already exported).
      2. The .env file at the project root.

    Raises RuntimeError if not found in either place.
    """
    dsn = os.environ.get("DATABASE_URL")
    if not dsn:
        env_from_file = load_env_file(ENV_FILE)
        dsn = env_from_file.get("DATABASE_URL")
    if not dsn:
        raise RuntimeError(
            "DATABASE_URL is not set in the environment nor in .env. "
            "Expected format: postgresql://user:password@host:port/db"
        )
    conn = psycopg.connect(dsn)
    register_vector(conn)
    return conn

def fetch_candidates(
        conn: psycopg.Connection,
        limit: int | None,
        logger: logging.Logger,
) -> list[tuple[int, str, str, list[str], list[str], str | None]]:
    """
    Return (movie_id, title, overview, genres, keywords, stored_hash)
    for each candidate movie.

    Genres and keywords are aggregated from movie_genres and
    movie_keywords as arrays of names. They will be appended to the
    text fed to the embedding model so the vector captures categorical
    information explicitly, not just whatever happens to be mentioned
    in the synopsis.
    """
    sql = """
          SELECT
              m.id,
              m.title,
              m.overview,
              COALESCE(
                      (SELECT ARRAY_AGG(g.name ORDER BY g.name)
                       FROM movie_genres mg
                                JOIN genres g ON g.id = mg.genre_id
                       WHERE mg.movie_id = m.id),
                      ARRAY[]::TEXT[]
              ) AS genres,
              COALESCE(
                      (SELECT ARRAY_AGG(k.name ORDER BY k.name)
                       FROM movie_keywords mk
                                JOIN keywords k ON k.id = mk.keyword_id
                       WHERE mk.movie_id = m.id),
                      ARRAY[]::TEXT[]
              ) AS keywords,
              me.source_text_hash
          FROM movies m
                   LEFT JOIN movie_embeddings me ON me.movie_id = m.id
          WHERE m.overview IS NOT NULL
            AND LENGTH(m.overview) >= %s
          ORDER BY m.popularity DESC NULLS LAST, m.id ASC \
          """
    params: list = [MIN_OVERVIEW_LENGTH]
    if limit is not None:
        sql += " LIMIT %s"
        params.append(limit)

    with conn.cursor() as cur:
        cur.execute(sql, params)
        rows = cur.fetchall()
    logger.info(f"Fetched {len(rows):,} candidate movies (overview >= {MIN_OVERVIEW_LENGTH} chars)")
    return rows

def upsert_embeddings(
        conn: psycopg.Connection,
        batch: list[tuple[int, np.ndarray, str, str]],
) -> None:
    """
    Upsert a batch of (movie_id, embedding, model_name, source_text_hash)
    rows into movie_embeddings.

    ON CONFLICT (movie_id) DO UPDATE: when a row for this movie already
    exists, update the embedding, hash, model_name and generated_at.
    This is what makes the script idempotent across reruns.
    """
    sql = """
          INSERT INTO movie_embeddings
              (movie_id, embedding, model_name, source_text_hash, generated_at)
          VALUES (%s, %s, %s, %s, NOW())
              ON CONFLICT (movie_id) DO UPDATE SET
              embedding        = EXCLUDED.embedding,
                                            model_name       = EXCLUDED.model_name,
                                            source_text_hash = EXCLUDED.source_text_hash,
                                            generated_at     = EXCLUDED.generated_at \
          """
    with conn.cursor() as cur:
        cur.executemany(sql, batch)


# ----------------------------------------------------------------------------
# Text building and hashing
# ----------------------------------------------------------------------------

def build_source_text(
        title: str,
        overview: str,
        genres: list[str],
        keywords: list[str],
) -> str:
    """
    Build the text fed to the embedding model.

    Strategy: concatenate four signals separated by periods so the
    model treats them as adjacent sentences:
      1. Title          (e.g. "Fight Club")
      2. Genres         (e.g. "Drama, Thriller")
      3. Keywords       (e.g. "dual identity, insomnia, masculinity, ...")
      4. Overview       (the actual synopsis)

    Why include genres and keywords:
    Pure-overview embeddings tend to latch onto the most salient
    surface words of the synopsis (e.g. "fight club" or "thief"),
    missing thematic concepts that the synopsis does not spell out.
    Including the curated category labels and keyword tags injects
    that information explicitly, producing embeddings that reflect
    both the literal description and the editorial classification.

    Keywords are capped at 20 to keep the text within the model's
    256-token input window without sacrificing the synopsis.
    """
    title = (title or "").strip()
    overview = (overview or "").strip()

    parts: list[str] = []

    if title:
        parts.append(title)

    if genres:
        parts.append("Genres: " + ", ".join(genres))

    if keywords:
        # Cap keywords to avoid pushing overview out of the input window.
        # The 20-keyword cap leaves ~100 tokens for the synopsis on
        # average, which is enough for the great majority of overviews.
        capped = keywords[:20]
        parts.append("Keywords: " + ", ".join(capped))

    if overview:
        parts.append(overview)

    return ". ".join(parts)


def hash_text(text: str) -> str:
    """SHA-256 of the UTF-8 encoded text, hex-encoded for storage as TEXT."""
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


# ----------------------------------------------------------------------------
# Main pipeline
# ----------------------------------------------------------------------------

def chunked(seq: list, size: int) -> Iterator[list]:
    """Yield successive 'size'-sized chunks of seq."""
    for i in range(0, len(seq), size):
        yield seq[i:i + size]


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate movie embeddings.")
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Process only the N most popular candidates (useful for dry runs).",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Regenerate embeddings even if the source text hash matches.",
    )
    args = parser.parse_args()

    logger = setup_logging()
    t_start = time.time()

    # --- 1. Load the model ---
    logger.info(f"Loading model {MODEL_NAME}...")
    model = SentenceTransformer(MODEL_NAME)
    logger.info(f"Model loaded. Output dimension: {model.get_embedding_dimension()}")
    assert model.get_embedding_dimension() == EMBEDDING_DIM, \
        f"Model dimension does not match the database schema ({EMBEDDING_DIM})."

    # --- 2. Open connection and fetch candidates ---
    with open_connection() as conn:
        candidates = fetch_candidates(conn, args.limit, logger)

        # --- 3. Filter out candidates whose hash already matches ---
        # --- 3. Filter out candidates whose hash already matches ---
        to_process: list[tuple[int, str, str]] = []  # (id, text, hash)
        n_skipped = 0
        for movie_id, title, overview, genres, keywords, stored_hash in candidates:
            text = build_source_text(title, overview, genres, keywords)
            text_hash = hash_text(text)
            if not args.force and stored_hash == text_hash:
                n_skipped += 1
                continue
            to_process.append((movie_id, text, text_hash))

        logger.info(
            f"Skipped {n_skipped:,} movies (hash already current). "
            f"Will generate {len(to_process):,} embeddings."
        )

        if not to_process:
            logger.info("Nothing to do. Exiting.")
            return

        # --- 4. Encode in batches and upsert ---
        n_done = 0
        with tqdm(total=len(to_process), desc="Embedding", unit="movie") as pbar:
            for chunk in chunked(to_process, ENCODE_BATCH):
                ids = [row[0] for row in chunk]
                texts = [row[1] for row in chunk]
                hashes = [row[2] for row in chunk]

                # Encode the batch. convert_to_numpy=True gives us np.ndarray
                # which the pgvector adapter accepts directly.
                embeddings = model.encode(
                    texts,
                    batch_size=ENCODE_BATCH,
                    convert_to_numpy=True,
                    show_progress_bar=False,
                )

                # Build the rows for executemany and upsert in DB batches.
                rows = [
                    (movie_id, emb, MODEL_TAG, h)
                    for movie_id, emb, h in zip(ids, embeddings, hashes)
                ]
                for db_chunk in chunked(rows, DB_BATCH):
                    upsert_embeddings(conn, db_chunk)
    # Note: commit moved outside the inner loop to reduce roundtrips.
    # A single commit batches all inserts into one transaction,
    # avoiding per-batch fsync overhead in PostgreSQL.

                n_done += len(chunk)
                pbar.update(len(chunk))
                # Commit every ~1000 movies. If the script is interrupted we lose
                # at most the last 1000, which the next run will regenerate.
                if n_done % 1024 == 0:
                    conn.commit()

        elapsed = time.time() - t_start
        rate = n_done / elapsed if elapsed > 0 else 0
        logger.info(
            f"Generated {n_done:,} embeddings in {elapsed:.1f}s "
            f"({rate:.0f} movies/s)"
        )


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(130)