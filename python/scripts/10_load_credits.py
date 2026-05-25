"""
Load 'people', 'movie_cast' and 'movie_crew' tables from
data/processed/credits.csv.

Each row of credits.csv contains a movie_id plus two JSON-encoded
columns: 'cast' (list of actors) and 'crew' (list of technical roles).
This script extracts:

- Distinct persons (deduplicated by id) into 'people'.
- Cast junctions (movie_id, person_id, cast_order, character_name).
- Crew junctions (movie_id, person_id, job, department).

Deduplication is required on three levels:
- Persons across all movies (one row per person in 'people').
- Cast junctions within a single movie by (movie_id, person_id,
  cast_order), because TMDB occasionally duplicates entries.
- Crew junctions by (movie_id, person_id, job), same reason.

Junctions whose movie_id is not present in the movies table are
filtered out to avoid FK violations.

Usage:
    uv run python python/scripts/10_load_credits.py
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


CREDITS_FILE = DATA_PROCESSED / "credits.csv"


# =============================================================================
# Extraction
# =============================================================================

def extract_people_and_junctions(
        df: pd.DataFrame,
        logger,
) -> tuple[list[tuple], list[tuple], list[tuple]]:
    """
    Single-pass extraction over all rows of credits.csv.

    Returns three lists:
    - people:      distinct (id, name, gender, profile_path)
    - cast_rows:   (movie_id, person_id, cast_order, character_name)
    - crew_rows:   (movie_id, person_id, job, department)

    Cast and crew rows are deduplicated by their composite primary key
    to ensure no PK violations during COPY.
    """
    seen_people: set[int] = set()
    people: list[tuple] = []

    # Use a set to deduplicate junctions by their PK tuple
    cast_seen: set[tuple] = set()
    cast_rows: list[tuple] = []
    crew_seen: set[tuple] = set()
    crew_rows: list[tuple] = []

    for _, row in df.iterrows():
        movie_id = int(row["id"])

        # ---- Cast ----
        cast = parse_python_literal(row["cast"])
        if isinstance(cast, list):
            for entry in cast:
                if not isinstance(entry, dict):
                    continue
                pid = entry.get("id")
                pname = entry.get("name")
                if pid is None or pname is None:
                    continue
                pid_int = int(pid)

                if pid_int not in seen_people:
                    seen_people.add(pid_int)
                    people.append((
                        pid_int,
                        pname,
                        entry.get("gender"),
                        entry.get("profile_path"),
                    ))

                cast_order = entry.get("order")
                if cast_order is None:
                    continue
                cast_order = int(cast_order)
                key = (movie_id, pid_int, cast_order)
                if key in cast_seen:
                    continue
                cast_seen.add(key)
                cast_rows.append((
                    movie_id,
                    pid_int,
                    cast_order,
                    entry.get("character") or None,
                ))

        # ---- Crew ----
        crew = parse_python_literal(row["crew"])
        if isinstance(crew, list):
            for entry in crew:
                if not isinstance(entry, dict):
                    continue
                pid = entry.get("id")
                pname = entry.get("name")
                job = entry.get("job")
                if pid is None or pname is None or not job:
                    continue
                pid_int = int(pid)

                if pid_int not in seen_people:
                    seen_people.add(pid_int)
                    people.append((
                        pid_int,
                        pname,
                        entry.get("gender"),
                        entry.get("profile_path"),
                    ))

                key = (movie_id, pid_int, job)
                if key in crew_seen:
                    continue
                crew_seen.add(key)
                crew_rows.append((
                    movie_id,
                    pid_int,
                    job,
                    entry.get("department") or None,
                ))

    logger.info(
        f"Extracted: {len(people):,} distinct people, "
        f"{len(cast_rows):,} cast junctions, "
        f"{len(crew_rows):,} crew junctions"
    )
    return people, cast_rows, crew_rows


def filter_existing_movie_ids(
        conn,
        junctions: list[tuple],
        logger,
        label: str,
) -> list[tuple]:
    """Filter junctions to those whose movie_id exists in 'movies'."""
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM movies")
        valid_ids = {row[0] for row in cur.fetchall()}
    before = len(junctions)
    filtered = [j for j in junctions if j[0] in valid_ids]
    dropped = before - len(filtered)
    if dropped > 0:
        logger.info(
            f"Filtered {dropped:,} {label} junctions "
            f"with movie_id not in movies"
        )
    return filtered


# =============================================================================
# Main
# =============================================================================

def main() -> None:
    logger = setup_logging()
    logger.info(f"Reading {CREDITS_FILE.name}")

    df = pd.read_csv(CREDITS_FILE, low_memory=False)
    logger.info(f"Loaded {len(df):,} rows from credits CSV")

    people, cast_rows, crew_rows = extract_people_and_junctions(df, logger)

    conn = connect()
    try:
        # ---- Load people ----
        inserted = upsert_canonical_entities(
            conn,
            table="people",
            id_column="id",
            columns=["id", "name", "gender", "profile_path"],
            rows=people,
        )
        conn.commit()
        logger.info(f"people: {inserted:,} new inserted")

        # ---- Load cast junctions ----
        cast_rows = filter_existing_movie_ids(conn, cast_rows, logger, "cast")
        n = copy_rows(
            conn,
            "movie_cast",
            ["movie_id", "person_id", "cast_order", "character_name"],
            cast_rows,
        )
        conn.commit()
        logger.info(f"movie_cast: {n:,} rows loaded")

        # ---- Load crew junctions ----
        crew_rows = filter_existing_movie_ids(conn, crew_rows, logger, "crew")
        n = copy_rows(
            conn,
            "movie_crew",
            ["movie_id", "person_id", "job", "department"],
            crew_rows,
        )
        conn.commit()
        logger.info(f"movie_crew: {n:,} rows loaded")

    finally:
        conn.close()

    logger.info("Load step 10 complete")


if __name__ == "__main__":
    main()