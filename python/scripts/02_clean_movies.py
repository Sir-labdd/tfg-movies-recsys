"""
Clean movies_metadata.csv applying treatment strategies P1–P10 (from
the initial inspection in section 7.2.5 of the memoir) and P13–P17
(from the secondary audit documented in section 7.3.1).

Reads:  data/raw/movies_metadata.csv
Writes: data/processed/movies.csv

The cleaning pipeline runs in a fixed order. See docstring of each
function in src/cleaning.py for the rationale of each step.

Usage:
    uv run python python/scripts/02_clean_movies.py
"""

from pathlib import Path

import pandas as pd

from src.cleaning import (
    cast_types,
    clip_runtime,
    drop_duplicates_by_key,
    drop_shifted_rows,
    fill_status_unknown,
    filter_adult,
    mask_short_overviews,
    mask_suspicious_budget,
    mask_unreliable_ratings,
    normalize_whitespace,
    parse_release_date,
    replace_placeholders_with_nan,
    setup_logging,
    vote_average_to_nan_when_no_votes,
    zeros_to_nan,
)


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DATA_RAW = PROJECT_ROOT / "data" / "raw"
DATA_PROCESSED = PROJECT_ROOT / "data" / "processed"

INPUT_FILE = DATA_RAW / "movies_metadata.csv"
OUTPUT_FILE = DATA_PROCESSED / "movies.csv"

COLUMNS_TO_KEEP = [
    "id",
    "imdb_id",
    "title",
    "original_title",
    "original_language",
    "overview",
    "tagline",
    "release_date",
    "runtime",
    "budget",
    "revenue",
    "popularity",
    "vote_average",
    "vote_count",
    "status",
    "adult",
    "genres",
    "production_companies",
    "production_countries",
    "spoken_languages",
    "belongs_to_collection",
    "poster_path",
]


def main() -> None:
    logger = setup_logging()
    logger.info(f"Reading {INPUT_FILE.name}")

    df = pd.read_csv(INPUT_FILE, low_memory=False)
    logger.info(f"Loaded {len(df):,} rows, {len(df.columns)} columns")

    # --- Structural cleaning (P1, type casting, date parsing) ---
    df = drop_shifted_rows(df, logger)
    df = cast_types(
        df,
        int_cols=["id", "vote_count"],
        float_cols=["budget", "popularity", "runtime",
                    "revenue", "vote_average"],
        logger=logger,
    )
    df = parse_release_date(df, logger)

    # --- Adult content filtering (secondary audit) ---
    df = filter_adult(df, logger)

    # --- Hidden zeros to NaN (P2, P3, P4, P13) ---
    df = zeros_to_nan(df, columns=["budget", "revenue", "popularity"], logger=logger)
    df = vote_average_to_nan_when_no_votes(df, logger)
    df = zeros_to_nan(df, columns=["runtime"], logger=logger)
    df = clip_runtime(df, max_minutes=300, logger=logger)

    # --- Suspicious value masking (P16) ---
    df = mask_suspicious_budget(df, min_realistic=1000.0, logger=logger)

    # --- Text cleaning (P6, P7) ---
    df = normalize_whitespace(
        df,
        columns=["title", "original_title", "overview", "tagline"],
        logger=logger,
    )
    df = replace_placeholders_with_nan(df, column="overview", logger=logger)

    # --- Fill status (P14) ---
    df = fill_status_unknown(df, logger)

    # --- Deduplication (P9) ---
    df = drop_duplicates_by_key(df, key="imdb_id", logger=logger)

    # --- Column selection and persistence ---
    available = [c for c in COLUMNS_TO_KEEP if c in df.columns]
    missing = set(COLUMNS_TO_KEEP) - set(available)
    if missing:
        logger.warning(f"Columns missing from input: {missing}")
    df = df[available]

    DATA_PROCESSED.mkdir(parents=True, exist_ok=True)
    df.to_csv(OUTPUT_FILE, index=False)

    logger.info(f"Wrote {len(df):,} rows to {OUTPUT_FILE}")
    logger.info(f"Final shape: {df.shape}")
    logger.info(f"Final memory: "
                f"{df.memory_usage(deep=True).sum() / 1024**2:.1f} MB")

    # Sanity check on key columns
    logger.info("Sanity check on key numeric columns:")
    for col in ["budget", "revenue", "runtime", "vote_average", "popularity"]:
        n_real = df[col].notna().sum()
        pct = 100 * n_real / len(df)
        logger.info(f"  {col}: {n_real:,} real values ({pct:.1f}%)")


if __name__ == "__main__":
    main()