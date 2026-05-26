"""
Load 'external_ids' table from data/processed/links.csv.

links.csv has one row per movie with three columns:
- movieId   (MovieLens id)
- imdbId    (IMDb numeric id, e.g. 114709 — needs 'tt' prefix and zero-padding)
- tmdbId    (TMDB id, the one we use as primary key)

The 'movies' table is keyed by TMDB id, so tmdbId is the FK. Rows with
no matching movie are filtered out (this happens when a movie was
deduplicated during cleaning).

The IMDb id is stored as full text ('tt0114709') for compatibility
with the existing 'imdb_id' column in 'movies'.

Usage:
    uv run python python/scripts/11_load_links.py
"""

import pandas as pd

from src.cleaning import setup_logging
from src.loading import (
    DATA_PROCESSED,
    connect,
    copy_rows,
)


LINKS_FILE = DATA_PROCESSED / "links.csv"


def format_imdb_id(imdb_numeric) -> str | None:
    """
    Convert a numeric IMDb id (114709) to its canonical text form
    ('tt0114709'), zero-padded to 7 digits.
    """
    if pd.isna(imdb_numeric):
        return None
    return f"tt{int(imdb_numeric):07d}"


def main() -> None:
    logger = setup_logging()
    logger.info(f"Reading {LINKS_FILE.name}")

    df = pd.read_csv(LINKS_FILE)
    logger.info(f"Loaded {len(df):,} rows from links CSV")

    # Build the rows in the order of external_ids columns:
    # (movie_id, movielens_id, imdb_id_full)
    rows: list[tuple] = []
    for _, r in df.iterrows():
        if pd.isna(r["tmdbId"]):
            continue
        rows.append((
            int(r["tmdbId"]),
            int(r["movieId"]) if pd.notna(r["movieId"]) else None,
            format_imdb_id(r["imdbId"]),
        ))
    logger.info(f"Prepared {len(rows):,} candidate rows")

    conn = connect()
    try:
        # Filter to existing movie IDs
        with conn.cursor() as cur:
            cur.execute("SELECT id FROM movies")
            valid_ids = {row[0] for row in cur.fetchall()}
        before = len(rows)
        rows = [r for r in rows if r[0] in valid_ids]
        dropped = before - len(rows)
        if dropped > 0:
            logger.info(
                f"Filtered out {dropped:,} links rows "
                f"with tmdbId not in movies"
            )

        n = copy_rows(
            conn,
            "external_ids",
            ["movie_id", "movielens_id", "imdb_id_full"],
            rows,
        )
        conn.commit()
        logger.info(f"external_ids: {n:,} rows loaded")

    finally:
        conn.close()

    logger.info("Load step 11 complete")


if __name__ == "__main__":
    main()