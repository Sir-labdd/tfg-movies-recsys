"""
Clean links.csv applying treatment strategies documented in section
7.2.5 of the memoir, plus P12 from the secondary audit.

Reads:  data/raw/links.csv
Writes: data/processed/links.csv

Operations:
1. Drop rows with NaN tmdbId (cannot be linked to movies_metadata).
2. Cast tmdbId from float to Int64.
3. Deduplicate by tmdbId, keeping the first occurrence. The same
   TMDB film should map to a single MovieLens entry; duplicates
   point to inconsistencies in the original dataset and would break
   the unique constraint when loaded into the database. (P12)

Note: links_small.csv is deliberately not processed because it is not
a strict subset of links.csv (10 IDs are unique to the small version,
36,728 are unique to the full one). The full version is the canonical
mapping.

Usage:
    uv run python python/scripts/05_clean_links.py
"""

from pathlib import Path

import pandas as pd

from src.cleaning import (
    drop_duplicates_by_key,
    log_dataframe_change,
    setup_logging,
)


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DATA_RAW = PROJECT_ROOT / "data" / "raw"
DATA_PROCESSED = PROJECT_ROOT / "data" / "processed"

INPUT_FILE = DATA_RAW / "links.csv"
OUTPUT_FILE = DATA_PROCESSED / "links.csv"


def main() -> None:
    logger = setup_logging()
    logger.info(f"Reading {INPUT_FILE.name}")

    df = pd.read_csv(INPUT_FILE)
    logger.info(f"Loaded {len(df):,} rows, {len(df.columns)} columns")

    before = len(df)
    df = df.dropna(subset=["tmdbId"]).copy()
    log_dataframe_change(logger, "drop rows with NaN tmdbId", before, len(df))

    df["tmdbId"] = df["tmdbId"].astype("Int64")

    df = drop_duplicates_by_key(df, key="tmdbId", logger=logger)

    DATA_PROCESSED.mkdir(parents=True, exist_ok=True)
    df.to_csv(OUTPUT_FILE, index=False)

    logger.info(f"Wrote {len(df):,} rows to {OUTPUT_FILE}")


if __name__ == "__main__":
    main()