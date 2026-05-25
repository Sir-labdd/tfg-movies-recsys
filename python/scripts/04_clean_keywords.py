"""
Clean keywords.csv applying treatment strategy P8 documented in section
7.2.5 of the memoir.

Reads:  data/raw/keywords.csv
Writes: data/processed/keywords.csv

The only quality issue in this file is the disproportionately high
volume of full-row duplicates (987 according to the inspection).
Deduplication keeps the first occurrence.

The 'keywords' column is kept as its raw JSON-encoded string. Its
normalization to a relational table will happen in a later block.

Usage:
    uv run python python/scripts/04_clean_keywords.py
"""

from pathlib import Path

import pandas as pd

from src.cleaning import drop_full_duplicates, setup_logging


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DATA_RAW = PROJECT_ROOT / "data" / "raw"
DATA_PROCESSED = PROJECT_ROOT / "data" / "processed"

INPUT_FILE = DATA_RAW / "keywords.csv"
OUTPUT_FILE = DATA_PROCESSED / "keywords.csv"


def main() -> None:
    logger = setup_logging()
    logger.info(f"Reading {INPUT_FILE.name}")

    df = pd.read_csv(INPUT_FILE, low_memory=False)
    logger.info(f"Loaded {len(df):,} rows, {len(df.columns)} columns")

    df = drop_full_duplicates(df, logger)

    DATA_PROCESSED.mkdir(parents=True, exist_ok=True)
    df.to_csv(OUTPUT_FILE, index=False)

    logger.info(f"Wrote {len(df):,} rows to {OUTPUT_FILE}")


if __name__ == "__main__":
    main()