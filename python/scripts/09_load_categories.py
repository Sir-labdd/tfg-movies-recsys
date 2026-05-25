"""
Load the five category families and their many-to-many junctions:
- genres + movie_genres
- production_companies + movie_production_companies
- production_countries + movie_production_countries
- spoken_languages + movie_spoken_languages
- keywords + movie_keywords

The first four families come from columns in movies.csv (already JSON-
encoded). The fifth family comes from keywords.csv (one row per movie
with a list of keywords).

Each family follows the same pattern:
1. Iterate over all source rows, parsing the JSON-encoded list of
   entities.
2. Accumulate distinct entities (deduplicated by id) and accumulate
   junction rows (movie_id, entity_id).
3. INSERT entities with ON CONFLICT DO NOTHING (idempotent).
4. COPY junction rows in bulk.

Usage:
    uv run python python/scripts/09_load_categories.py
"""

from pathlib import Path
from typing import Any

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
KEYWORDS_FILE = DATA_PROCESSED / "keywords.csv"


# =============================================================================
# Generic processor for {id, name}-style families
# =============================================================================

def collect_id_name_entities(
        df: pd.DataFrame,
        source_column: str,
        id_column: str,
        movie_id_column: str = "id",
) -> tuple[list[tuple], list[tuple]]:
    """
    Walk a DataFrame whose 'source_column' contains a list of dicts
    with at least {'id', 'name'} fields. Return two lists:
    - entities: distinct (id, name) tuples
    - junctions: (movie_id, entity_id) tuples preserving relations

    The id_column parameter is the name used in the dicts (always 'id'
    here, but kept explicit for clarity).
    """
    seen_ids: set[int] = set()
    entities: list[tuple] = []
    junctions: list[tuple] = []

    for _, row in df.iterrows():
        movie_id = int(row[movie_id_column])
        parsed = parse_python_literal(row[source_column])
        if not isinstance(parsed, list):
            continue
        for item in parsed:
            if not isinstance(item, dict):
                continue
            eid = item.get(id_column)
            name = item.get("name")
            if eid is None or name is None:
                continue
            eid_int = int(eid)
            if eid_int not in seen_ids:
                seen_ids.add(eid_int)
                entities.append((eid_int, name))
            junctions.append((movie_id, eid_int))

    return entities, junctions


def collect_iso_name_entities(
        df: pd.DataFrame,
        source_column: str,
        iso_key: str,
        movie_id_column: str = "id",
) -> tuple[list[tuple], list[tuple]]:
    """
    Variant for entities keyed by ISO code (countries, languages).
    The dicts have shape {iso_key: 'us', 'name': 'United States'}.
    """
    seen_iso: set[str] = set()
    entities: list[tuple] = []
    junctions: list[tuple] = []

    for _, row in df.iterrows():
        movie_id = int(row[movie_id_column])
        parsed = parse_python_literal(row[source_column])
        if not isinstance(parsed, list):
            continue
        for item in parsed:
            if not isinstance(item, dict):
                continue
            iso = item.get(iso_key)
            name = item.get("name")
            if not iso or not name:
                continue
            iso_norm = str(iso).strip().lower()
            # ISO codes should be 2 chars; defensive skip if not
            if len(iso_norm) != 2:
                continue
            if iso_norm not in seen_iso:
                seen_iso.add(iso_norm)
                entities.append((iso_norm, name))
            junctions.append((movie_id, iso_norm))

    return entities, junctions


# =============================================================================
# Per-family loaders
# =============================================================================

def load_genres(conn, movies_df, logger):
    entities, junctions = collect_id_name_entities(
        movies_df, source_column="genres", id_column="id"
    )
    inserted = upsert_canonical_entities(
        conn, "genres", "id", ["id", "name"], entities
    )
    conn.commit()
    logger.info(f"genres: {len(entities):,} distinct, {inserted:,} new inserted")

    n = copy_rows(conn, "movie_genres",
                  ["movie_id", "genre_id"], junctions)
    conn.commit()
    logger.info(f"movie_genres: {n:,} junctions loaded")


def load_production_companies(conn, movies_df, logger):
    entities, junctions = collect_id_name_entities(
        movies_df, source_column="production_companies", id_column="id"
    )
    inserted = upsert_canonical_entities(
        conn, "production_companies", "id", ["id", "name"], entities
    )
    conn.commit()
    logger.info(
        f"production_companies: {len(entities):,} distinct, "
        f"{inserted:,} new inserted"
    )

    n = copy_rows(conn, "movie_production_companies",
                  ["movie_id", "company_id"], junctions)
    conn.commit()
    logger.info(f"movie_production_companies: {n:,} junctions loaded")


def load_production_countries(conn, movies_df, logger):
    entities, junctions = collect_iso_name_entities(
        movies_df, source_column="production_countries", iso_key="iso_3166_1"
    )
    inserted = upsert_canonical_entities(
        conn, "production_countries", "iso_3166_1",
        ["iso_3166_1", "name"], entities
    )
    conn.commit()
    logger.info(
        f"production_countries: {len(entities):,} distinct, "
        f"{inserted:,} new inserted"
    )

    n = copy_rows(conn, "movie_production_countries",
                  ["movie_id", "country_iso"], junctions)
    conn.commit()
    logger.info(f"movie_production_countries: {n:,} junctions loaded")


def load_spoken_languages(conn, movies_df, logger):
    entities, junctions = collect_iso_name_entities(
        movies_df, source_column="spoken_languages", iso_key="iso_639_1"
    )
    inserted = upsert_canonical_entities(
        conn, "spoken_languages", "iso_639_1",
        ["iso_639_1", "name"], entities
    )
    conn.commit()
    logger.info(
        f"spoken_languages: {len(entities):,} distinct, "
        f"{inserted:,} new inserted"
    )

    n = copy_rows(conn, "movie_spoken_languages",
                  ["movie_id", "language_iso"], junctions)
    conn.commit()
    logger.info(f"movie_spoken_languages: {n:,} junctions loaded")


def load_keywords(conn, logger):
    """
    Keywords come from keywords.csv, not movies.csv. Reuse the same
    extractor by reading the file and feeding it through the same
    generic function.
    """
    logger.info(f"Reading {KEYWORDS_FILE.name}")
    kw_df = pd.read_csv(KEYWORDS_FILE, low_memory=False)
    logger.info(f"Loaded {len(kw_df):,} rows from keywords CSV")

    entities, junctions = collect_id_name_entities(
        kw_df, source_column="keywords", id_column="id"
    )
    inserted = upsert_canonical_entities(
        conn, "keywords", "id", ["id", "name"], entities
    )
    conn.commit()
    logger.info(f"keywords: {len(entities):,} distinct, {inserted:,} new inserted")

    # Filter junctions to those whose movie_id exists in movies table.
    # keywords.csv has 45,432 rows but movies has 45,408 after cleaning;
    # some junctions would violate the FK without this filter.
    junctions = _filter_existing_movie_ids(conn, junctions, logger)

    n = copy_rows(conn, "movie_keywords",
                  ["movie_id", "keyword_id"], junctions)
    conn.commit()
    logger.info(f"movie_keywords: {n:,} junctions loaded")


def _filter_existing_movie_ids(
        conn,
        junctions: list[tuple],
        logger,
) -> list[tuple]:
    """
    Return only junctions whose movie_id exists in the movies table.
    Avoids FK violations when source CSVs are slightly out of sync
    after independent deduplication.
    """
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM movies")
        valid_ids = {row[0] for row in cur.fetchall()}
    before = len(junctions)
    filtered = [j for j in junctions if j[0] in valid_ids]
    dropped = before - len(filtered)
    if dropped > 0:
        logger.info(
            f"Filtered out {dropped:,} junctions with movie_id not in movies"
        )
    return filtered


# =============================================================================
# Main
# =============================================================================

def main() -> None:
    logger = setup_logging()
    logger.info(f"Reading {MOVIES_FILE.name}")

    movies_df = pd.read_csv(MOVIES_FILE, low_memory=False)
    logger.info(f"Loaded {len(movies_df):,} movie rows")

    # Same FK filter we use in keywords applies to all families
    # extracted from movies.csv: junctions reference movies.id which
    # should be a subset already, but we sanitize just in case.

    conn = connect()
    try:
        load_genres(conn, movies_df, logger)
        load_production_companies(conn, movies_df, logger)
        load_production_countries(conn, movies_df, logger)
        load_spoken_languages(conn, movies_df, logger)
        load_keywords(conn, logger)
    finally:
        conn.close()

    logger.info("Load step 09 complete")


if __name__ == "__main__":
    main()