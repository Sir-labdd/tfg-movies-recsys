"""
Generic cleaning utilities for the TFG dataset pipeline.

This module provides reusable functions applied across the per-file
cleaning scripts in python/scripts/. Each function corresponds to one
or more treatment strategies documented in section 7.2.5 of the memoir
(catalog of problems P1 through P10).

Design principles:
- Functions are pure when possible: they take a DataFrame, return a new
  DataFrame. They do not mutate the input.
- All functions emit a structured log message reporting how many rows
  or values were affected, so that the pipeline output remains auditable.
- Type hints are provided to enable static analysis and IDE assistance.
"""

import ast
import logging
import re
from typing import Any

import numpy as np
import pandas as pd


# =============================================================================
# Logging setup
# =============================================================================

def setup_logging(level: int = logging.INFO) -> logging.Logger:
    """
    Configure the root logger with a consistent format for all pipeline
    scripts.

    Should be called once at the top of each script's main() function.
    """
    logging.basicConfig(
        level=level,
        format="%(asctime)s | %(levelname)-7s | %(message)s",
        datefmt="%H:%M:%S",
    )
    return logging.getLogger("pipeline")


def log_dataframe_change(
        logger: logging.Logger,
        label: str,
        before: int,
        after: int,
) -> None:
    """
    Report how many rows were removed by an operation, as both absolute
    count and percentage of the input.
    """
    removed = before - after
    pct = (100 * removed / before) if before else 0
    logger.info(
        f"{label}: {before:,} -> {after:,} rows "
        f"({removed:,} removed, {pct:.2f}%)"
    )


# =============================================================================
# Category 1: Structural cleaning
# =============================================================================

def drop_shifted_rows(df: pd.DataFrame, logger: logging.Logger) -> pd.DataFrame:
    """
    Remove rows where 'id' is not convertible to an integer.

    These rows are produced by unescaped commas or newlines in the
    source CSV, causing horizontal column shift. They represent ~0.007%
    of the dataset and are filtered cleanly by the predictable type
    of the 'id' column. (P1)
    """
    before = len(df)
    id_numeric = pd.to_numeric(df["id"], errors="coerce")
    df_clean = df[id_numeric.notna()].copy()
    log_dataframe_change(logger, "drop_shifted_rows", before, len(df_clean))
    return df_clean


def cast_types(
        df: pd.DataFrame,
        int_cols: list[str] | None = None,
        float_cols: list[str] | None = None,
        logger: logging.Logger | None = None,
) -> pd.DataFrame:
    """
    Cast columns to their intended types after structural cleaning has
    already removed malformed rows.

    Use pd.to_numeric with errors='coerce' so any remaining bad values
    become NaN instead of crashing.
    """
    df = df.copy()
    for col in int_cols or []:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors="coerce").astype("Int64")
    for col in float_cols or []:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors="coerce")
    if logger:
        casted = (int_cols or []) + (float_cols or [])
        logger.info(f"cast_types: cast {len(casted)} columns to numeric")
    return df


def parse_release_date(df: pd.DataFrame, logger: logging.Logger) -> pd.DataFrame:
    """
    Convert 'release_date' from string to pandas datetime.

    Unparseable strings become NaT. The structural cleaning step should
    have removed the three known unparseable rows already, but we are
    defensive here in case future versions of the dataset have new
    malformed entries.
    """
    df = df.copy()
    before_nulls = df["release_date"].isna().sum()
    df["release_date"] = pd.to_datetime(df["release_date"], errors="coerce")
    after_nulls = df["release_date"].isna().sum()
    new_nulls = after_nulls - before_nulls
    logger.info(
        f"parse_release_date: parsed to datetime, "
        f"{new_nulls} additional values became NaT"
    )
    return df


# =============================================================================
# Category 2: Hidden zeros to NaN
# =============================================================================

def zeros_to_nan(
        df: pd.DataFrame,
        columns: list[str],
        logger: logging.Logger,
) -> pd.DataFrame:
    """
    Replace zeros with NaN in the specified columns.

    Used for budget, revenue, and runtime (P2, P4) where a value of
    zero in the source data does not represent a real zero but rather
    a missing entry.
    """
    df = df.copy()
    for col in columns:
        if col not in df.columns:
            continue
        n_zeros = (df[col] == 0).sum()
        df.loc[df[col] == 0, col] = np.nan
        logger.info(f"zeros_to_nan[{col}]: replaced {n_zeros:,} zeros with NaN")
    return df


def vote_average_to_nan_when_no_votes(
        df: pd.DataFrame,
        logger: logging.Logger,
) -> pd.DataFrame:
    """
    Set 'vote_average' to NaN whenever 'vote_count' is zero or NaN.

    A vote_average of 0 with 0 votes is not a rating of zero; it's the
    absence of ratings. Conflating them would teach the predictive
    model that movies with no votes rate zero, which is meaningless. (P3)
    """
    df = df.copy()
    mask = (df["vote_count"].isna()) | (df["vote_count"] == 0)
    n_affected = mask.sum()
    df.loc[mask, "vote_average"] = np.nan
    logger.info(
        f"vote_average_to_nan_when_no_votes: "
        f"masked {n_affected:,} entries (no votes)"
    )
    return df


def clip_runtime(
        df: pd.DataFrame,
        max_minutes: int = 300,
        logger: logging.Logger | None = None,
) -> pd.DataFrame:
    """
    Mark as NaN any runtime greater than max_minutes.

    Such values are implausibly long for a film (default 5 hours)
    and represent miscataloged content (compilations, TV series, etc).
    (P4)
    """
    df = df.copy()
    if "runtime" not in df.columns:
        return df
    mask = df["runtime"] > max_minutes
    n_clipped = mask.sum()
    df.loc[mask, "runtime"] = np.nan
    if logger:
        logger.info(
            f"clip_runtime: masked {n_clipped:,} runtimes > {max_minutes} min"
        )
    return df


# =============================================================================
# Category 3: Text cleaning
# =============================================================================

# Compile regex once at module load for efficiency
_RE_MULTI_SPACE = re.compile(r"\s+")


def normalize_whitespace(
        df: pd.DataFrame,
        columns: list[str],
        logger: logging.Logger,
) -> pd.DataFrame:
    """
    Normalize whitespace in the specified text columns.

    Specifically:
    - Replace non-breaking spaces with regular spaces.
    - Collapse multiple consecutive whitespace characters into a single
      space.
    - Strip leading and trailing whitespace.

    Critical for the embedding-based recommender (P6): two textually
    identical sinopsis with different whitespace produce slightly
    different embeddings.
    """
    df = df.copy()
    for col in columns:
        if col not in df.columns:
            continue
        mask_notna = df[col].notna()
        # Replace non-breaking spaces, collapse whitespace, then strip
        df.loc[mask_notna, col] = (
            df.loc[mask_notna, col]
            .astype(str)
            .str.replace("\xa0", " ", regex=False)
            .str.replace(_RE_MULTI_SPACE, " ", regex=True)
            .str.strip()
        )
        logger.info(f"normalize_whitespace[{col}]: applied to {mask_notna.sum():,} entries")
    return df


# Known placeholder strings in 'overview' that mean "no real synopsis"
_PLACEHOLDER_PHRASES = {
    "n/a",
    "no overview found.",
    "no overview found",
    "tbd",
    "no description",
    "no description available",
    "no overview",
    "-",
    "none",
}


def replace_placeholders_with_nan(
        df: pd.DataFrame,
        column: str,
        logger: logging.Logger,
) -> pd.DataFrame:
    """
    Replace placeholder strings (case-insensitive) with NaN in the
    specified column.

    Without this step, the embedding model would generate a 'placeholder
    embedding' shared by all movies with no real synopsis, falsely
    flagging them as mutually similar in the recommender. (P7)
    """
    df = df.copy()
    if column not in df.columns:
        return df
    mask = df[column].fillna("").str.lower().str.strip().isin(_PLACEHOLDER_PHRASES)
    n_affected = mask.sum()
    df.loc[mask, column] = np.nan
    logger.info(
        f"replace_placeholders_with_nan[{column}]: "
        f"masked {n_affected:,} placeholder entries"
    )
    return df


# =============================================================================
# Category 4: Deduplication
# =============================================================================

def drop_full_duplicates(
        df: pd.DataFrame,
        logger: logging.Logger,
) -> pd.DataFrame:
    """
    Remove rows that are exact duplicates across all columns, keeping
    the first occurrence. (P8)
    """
    before = len(df)
    df_clean = df.drop_duplicates(keep="first").copy()
    log_dataframe_change(logger, "drop_full_duplicates", before, len(df_clean))
    return df_clean


def drop_duplicates_by_key(
        df: pd.DataFrame,
        key: str | list[str],
        logger: logging.Logger,
) -> pd.DataFrame:
    """
    Remove rows duplicated by the specified key column(s), keeping the
    first occurrence.

    Used for cases where the same logical entity is represented by
    multiple rows with different IDs (e.g. same movie uploaded twice
    to TMDB with different ids but matching imdb_id). (P9)
    """
    before = len(df)
    df_clean = df.drop_duplicates(subset=key, keep="first").copy()
    label = f"drop_duplicates_by_key[{key}]"
    log_dataframe_change(logger, label, before, len(df_clean))
    return df_clean


# =============================================================================
# Category 5: Utilities for future blocks
# =============================================================================

def parse_json_column_safe(value: Any) -> Any:
    """
    Parse a value that should contain a Python literal (typically a list
    of dicts) from a CSV cell.

    The CSVs in this dataset use single quotes (Python literal repr),
    not strict JSON. ast.literal_eval handles this correctly and is
    safer than eval() since it only evaluates literals.

    Returns the parsed structure on success, or an empty list on
    failure or NaN input.

    Not used yet in the cleaning step but kept here for the next block,
    when we normalize nested JSON columns (genres, cast, crew) into
    separate relational tables.
    """
    if pd.isna(value):
        return []
    try:
        result = ast.literal_eval(value)
        return result if isinstance(result, list) else []
    except (ValueError, SyntaxError):
        return []


# =============================================================================
# Category 6: Second-pass cleaning
# =============================================================================

def filter_adult(df: pd.DataFrame, logger: logging.Logger) -> pd.DataFrame:
    """
    Remove rows where adult is True (or 'True' as string).

    Adult content is excluded from the catalog at this stage to keep
    the application appropriate for an academic context.

    Handles both boolean and string-encoded values of the 'adult'
    column, since the raw CSV may store it as text after structural
    cleaning but before explicit type casting.
    """
    before = len(df)
    if "adult" not in df.columns:
        return df

    # Normalize to boolean: accept True, 'True', 'true', etc.
    adult_normalized = (
        df["adult"]
        .astype(str)
        .str.strip()
        .str.lower()
        .isin(["true", "1"])
    )
    df_clean = df[~adult_normalized].copy()
    log_dataframe_change(logger, "filter_adult", before, len(df_clean))
    return df_clean

def mask_suspicious_budget(
        df: pd.DataFrame,
        min_realistic: float = 1000.0,
        logger: logging.Logger | None = None,
) -> pd.DataFrame:
    """
    Mark as NaN any budget below a realistic threshold.

    Some entries report budgets of 1 to 100 dollars (e.g. Modern Times
    with budget=1$), which are clearly typographical errors in the
    source data. The threshold is set conservatively low (1000$) to
    avoid excluding genuine ultra-low-budget productions. (P16)
    """
    df = df.copy()
    if "budget" not in df.columns:
        return df
    mask = (df["budget"].notna()) & (df["budget"] < min_realistic) & (df["budget"] > 0)
    n_affected = mask.sum()
    df.loc[mask, "budget"] = np.nan
    if logger:
        logger.info(
            f"mask_suspicious_budget: masked {n_affected:,} budgets < {min_realistic:.0f}$"
        )
    return df


def mask_unreliable_ratings(
        df: pd.DataFrame,
        min_votes: int = 10,
        logger: logging.Logger | None = None,
) -> pd.DataFrame:
    """
    Mark as NaN vote_average when vote_count is below a reliability
    threshold.

    A rating of 10/10 with only 1 vote is statistically meaningless
    and would mislead the predictive model. Setting min_votes=10
    drops ratings supported by fewer than 10 votes. The film stays
    in the catalog; only the vote_average becomes NaN. (P17)
    """
    df = df.copy()
    if "vote_count" not in df.columns or "vote_average" not in df.columns:
        return df
    mask = (df["vote_count"].notna()) & (df["vote_count"] < min_votes)
    n_affected = (mask & df["vote_average"].notna()).sum()
    df.loc[mask, "vote_average"] = np.nan
    if logger:
        logger.info(
            f"mask_unreliable_ratings: masked {n_affected:,} ratings "
            f"backed by < {min_votes} votes"
        )
    return df


def mask_short_overviews(
        df: pd.DataFrame,
        min_length: int = 30,
        column: str = "overview",
        logger: logging.Logger | None = None,
) -> pd.DataFrame:
    """
    Mark as NaN overviews shorter than min_length characters.

    Short overviews tend to be meta-commentary ('German Comedy',
    'Frisian movie from 2004') rather than real synopses. Such text
    produces meaningless embeddings for the content-based recommender.
    (P15)
    """
    df = df.copy()
    if column not in df.columns:
        return df
    mask = df[column].notna() & (df[column].astype(str).str.len() < min_length)
    n_affected = mask.sum()
    df.loc[mask, column] = np.nan
    if logger:
        logger.info(
            f"mask_short_overviews[{column}]: masked {n_affected:,} "
            f"overviews shorter than {min_length} chars"
        )
    return df


def fill_status_unknown(
        df: pd.DataFrame,
        logger: logging.Logger,
) -> pd.DataFrame:
    """
    Fill NaN values in 'status' with the literal 'Unknown'.

    Avoids NULL values in a column where the application UI needs
    to display something. (P14)
    """
    df = df.copy()
    if "status" not in df.columns:
        return df
    n_filled = df["status"].isna().sum()
    df["status"] = df["status"].fillna("Unknown")
    logger.info(f"fill_status_unknown: filled {n_filled:,} NaN entries with 'Unknown'")
    return df