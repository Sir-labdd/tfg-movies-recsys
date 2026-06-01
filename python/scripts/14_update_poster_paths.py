"""
Update movie poster paths from a newer TMDB dataset.

Reads the newer TMDB_movie_dataset_v11.csv and updates the poster_path
column in the movies table for all matching TMDB IDs. This fixes
expired poster URLs without changing any other data or schema.

Usage:
    1. Place TMDB_movie_dataset_v11.csv in data/raw/
    2. Run: uv run python python/scripts/14_update_poster_paths.py

The script is idempotent: running it twice produces the same result.
"""

import csv
import os
import sys
from pathlib import Path

import psycopg

from src.cleaning import setup_logging

PROJECT_ROOT = Path(__file__).resolve().parents[2]
NEW_DATASET = PROJECT_ROOT / "data" / "raw" / "TMDB_movie_dataset_v11.csv"


def load_new_posters(path: Path, logger) -> dict[str, str]:
    """Read the new CSV and return a dict of {tmdb_id: poster_path}."""
    posters = {}
    with open(path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            tid = (row.get("id") or "").strip()
            poster = (row.get("poster_path") or "").strip()
            if tid and poster:
                posters[tid] = poster
    logger.info(f"Loaded {len(posters)} poster paths from {path.name}")
    return posters


def update_posters(posters: dict[str, str], logger) -> None:
    """Update poster_path in the movies table."""
    database_url = os.environ.get("DATABASE_URL")
    if not database_url:
        env_file = PROJECT_ROOT / ".env"
        if env_file.exists():
            for line in env_file.read_text().splitlines():
                if "=" in line and not line.strip().startswith("#"):
                    k, v = line.split("=", 1)
                    os.environ[k.strip()] = v.strip()
            database_url = os.environ.get("DATABASE_URL")

    if not database_url:
        logger.error("DATABASE_URL not set. Check your .env file.")
        sys.exit(1)

    with psycopg.connect(database_url) as conn:
        with conn.cursor() as cur:
            # Get current poster paths from DB
            cur.execute("SELECT id, poster_path FROM movies")
            rows = cur.fetchall()
            logger.info(f"Found {len(rows)} movies in database")

            updated = 0
            newly_added = 0
            for movie_id, old_poster in rows:
                str_id = str(movie_id)
                if str_id in posters:
                    new_poster = posters[str_id]
                    if new_poster != old_poster:
                        cur.execute(
                            "UPDATE movies SET poster_path = %s WHERE id = %s",
                            (new_poster, movie_id),
                        )
                        if old_poster:
                            updated += 1
                        else:
                            newly_added += 1

            conn.commit()
            logger.info(f"Updated: {updated} poster paths")
            logger.info(f"Newly added: {newly_added} (previously null)")
            logger.info(f"Total changes: {updated + newly_added}")


def main() -> None:
    logger = setup_logging()

    if not NEW_DATASET.exists():
        logger.error(f"File not found: {NEW_DATASET}")
        logger.error("Place TMDB_movie_dataset_v11.csv in data/raw/")
        sys.exit(1)

    posters = load_new_posters(NEW_DATASET, logger)
    update_posters(posters, logger)
    logger.info("Done.")


if __name__ == "__main__":
    main()
