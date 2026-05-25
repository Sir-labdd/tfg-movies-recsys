"""
Clean credits.csv applying treatment strategies P8 (from initial
inspection) and P11 (from secondary audit).

Reads:  data/raw/credits.csv
Writes: data/processed/credits.csv

Two deduplication passes are required:
- drop_full_duplicates removes 37 exact-row duplicates (P8).
- drop_duplicates_by_key removes additional rows that share the same
  movie id but differ in cast/crew content (P11). The first occurrence
  is kept arbitrarily, since both versions are equally valid sources.

Usage:
    uv run python python/scripts/03_clean_credits.py
"""

from pathlib import Path

import pandas as pd

from src.cleaning import (
    drop_duplicates_by_key,
    drop_full_duplicates,
    setup_logging,
)


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DATA_RAW = PROJECT_ROOT / "data" / "raw"
DATA_PROCESSED = PROJECT_ROOT / "data" / "processed"

INPUT_FILE = DATA_RAW / "credits.csv"
OUTPUT_FILE = DATA_PROCESSED / "credits.csv"


def main() -> None:
    logger = setup_logging()
    logger.info(f"Reading {INPUT_FILE.name}")

    df = pd.read_csv(INPUT_FILE, low_memory=False)
    logger.info(f"Loaded {len(df):,} rows, {len(df.columns)} columns")

    df = drop_full_duplicates(df, logger)
    df = drop_duplicates_by_key(df, key="id", logger=logger)

    DATA_PROCESSED.mkdir(parents=True, exist_ok=True)
    df.to_csv(OUTPUT_FILE, index=False)

    logger.info(f"Wrote {len(df):,} rows to {OUTPUT_FILE}")


if __name__ == "__main__":
    main()