"""
Load 'collections' and 'movies' tables from data/processed/movies.csv.

Order:
1. Extract distinct collections from belongs_to_collection column.
2. Load collections into the 'collections' table.
3. Load movies into the 'movies' table.

The collection_id FK in 'movies' references the previously loaded
'collections' rows. Movies without a collection get NULL.

This script is idempotent: collections use ON CONFLICT DO NOTHING,
movies fail loudly if duplicate ids exist (which shouldn't happen
after the cleaning pipeline).

Usage:
    uv run python python/scripts/08_load_movies.py
"""

from pathlib import Path

import pandas as pd

from src.cleaning import setup_logging
from src.loading import (
    DATA_PROCESSED,
    connect,
    copy_rows,
    parse_python_literal,
    upsert_canonical_entities,
)


MOVIES_FILE = DATA_PROCESSED / "movies.csv"


def extract_collections(df: pd.DataFrame, logger) -> list[tuple]:
    """
    Parse belongs_to_collection column and return distinct (id, name,
    poster_path, backdrop_path) tuples.
    """
    seen_ids: set[int] = set()
    collections: list[tuple] = []

    for value in df["belongs_to_collection"]:
        parsed = parse_python_literal(value)
        if parsed is None or not isinstance(parsed, dict):
            continue
        cid = parsed.get("id")
        if cid is None or cid in seen_ids:
            continue
        seen_ids.add(cid)
        collections.append((
            int(cid),
            parsed.get("name"),
            parsed.get("poster_path"),
            parsed.get("backdrop_path"),
        ))

    logger.info(f"Extracted {len(collections):,} distinct collections")
    return collections


def collection_id_for(value) -> int | None:
    """Extract the collection id from a belongs_to_collection cell."""
    parsed = parse_python_literal(value)
    if parsed is None or not isinstance(parsed, dict):
        return None
    cid = parsed.get("id")
    return int(cid) if cid is not None else None


def prepare_movie_rows(df: pd.DataFrame, logger) -> list[tuple]:
    """Build the list of tuples ready for COPY into the movies table."""
    rows: list[tuple] = []

    for _, r in df.iterrows():
        rows.append((
            int(r["id"]),
            r["imdb_id"] if pd.notna(r["imdb_id"]) else None,
            r["title"] if pd.notna(r["title"])
            else (r["original_title"] if pd.notna(r["original_title"]) else "Untitled"),
            r["original_title"] if pd.notna(r["original_title"]) else None,
            r["original_language"] if pd.notna(r["original_language"]) else None,
            r["overview"] if pd.notna(r["overview"]) else None,
            r["tagline"] if pd.notna(r["tagline"]) else None,
            r["release_date"] if pd.notna(r["release_date"]) else None,
            int(r["runtime"]) if pd.notna(r["runtime"]) else None,
            int(r["budget"]) if pd.notna(r["budget"]) else None,
            int(r["revenue"]) if pd.notna(r["revenue"]) else None,
            float(r["popularity"]) if pd.notna(r["popularity"]) else None,
            float(r["vote_average"]) if pd.notna(r["vote_average"]) else None,
            int(r["vote_count"]) if pd.notna(r["vote_count"]) else None,
            r["status"] if pd.notna(r["status"]) else "Unknown",
            bool(r["adult"]) if pd.notna(r["adult"]) else False,
            r["poster_path"] if pd.notna(r["poster_path"]) else None,
            collection_id_for(r["belongs_to_collection"]),
        ))

    logger.info(f"Prepared {len(rows):,} movie rows for COPY")
    return rows


MOVIE_COLUMNS = [
    "id", "imdb_id", "title", "original_title", "original_language",
    "overview", "tagline", "release_date", "runtime",
    "budget", "revenue", "popularity",
    "vote_average", "vote_count", "status", "adult",
    "poster_path", "collection_id",
]


def main() -> None:
    logger = setup_logging()
    logger.info(f"Reading {MOVIES_FILE}")

    df = pd.read_csv(MOVIES_FILE, low_memory=False)
    logger.info(f"Loaded {len(df):,} rows from CSV")

    conn = connect()
    try:
        # ---- Step 1: collections ----
        collections = extract_collections(df, logger)
        inserted = upsert_canonical_entities(
            conn,
            table="collections",
            id_column="id",
            columns=["id", "name", "poster_path", "backdrop_path"],
            rows=collections,
        )
        conn.commit()
        logger.info(f"Inserted {inserted:,} new collections")

        # ---- Step 2: movies ----
        movie_rows = prepare_movie_rows(df, logger)
        n_copied = copy_rows(
            conn,
            table="movies",
            columns=MOVIE_COLUMNS,
            rows=movie_rows,
        )
        conn.commit()
        logger.info(f"COPY into movies: {n_copied:,} rows")

    finally:
        conn.close()

    logger.info("Load step 08 complete")


if __name__ == "__main__":
    main()