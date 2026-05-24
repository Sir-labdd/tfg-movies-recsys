"""
Initial inspection of the raw dataset (v3).

Performs three levels of checks:
1. Known-issue checks: problems suspected in advance.
2. Broad sweep: per-column stats applied generically.
3. Targeted checks: specific anomalies known to occur in this dataset.

Output is human-readable on stdout. Real figures from this script feed
section 7.2 of the memoir.

Usage:
    python3 python/scripts/01_inspect_raw.py
"""

import ast
import re
from pathlib import Path

import pandas as pd

PROJECT_ROOT = Path(__file__).resolve().parents[2]
DATA_RAW = PROJECT_ROOT / "data" / "raw"


# =============================================================================
# Pretty printing helpers
# =============================================================================

def section(title: str) -> None:
    print()
    print("=" * 70)
    print(f"  {title}")
    print("=" * 70)


def subsection(title: str) -> None:
    print()
    print(f"--- {title} ---")


# =============================================================================
# Generic checks (apply to any file)
# =============================================================================

def report_basic_stats(df: pd.DataFrame) -> None:
    print(f"Rows:    {len(df):,}")
    print(f"Cols:    {len(df.columns)}")
    print(f"Memory:  {df.memory_usage(deep=True).sum() / 1024**2:.1f} MB")
    print()
    print("Columns and detected dtypes:")
    for col in df.columns:
        print(f"  - {col:30s} {df[col].dtype}")


def report_missing(df: pd.DataFrame) -> None:
    missing = df.isna().sum().sort_values(ascending=False)
    has_missing = missing[missing > 0]
    if has_missing.empty:
        print("No missing values in any column.")
        return
    print(f"Columns with missing values: {len(has_missing)} of {len(df.columns)}")
    for col, n in has_missing.items():
        pct = 100 * n / len(df)
        print(f"  - {col:30s} {n:>7,} ({pct:5.1f}%)")


def report_duplicates(df: pd.DataFrame, key_columns: list[str] | None = None) -> None:
    full_dups = df.duplicated().sum()
    print(f"Full-row duplicates: {full_dups}")
    if key_columns:
        for key in key_columns:
            if key in df.columns:
                key_dups = df[key].duplicated(keep=False).sum()
                print(f"Rows involved in duplication by '{key}': {key_dups}")


def report_numeric_anomalies(df: pd.DataFrame, columns: list[str]) -> None:
    for col in columns:
        if col not in df.columns:
            continue
        series = pd.to_numeric(df[col], errors="coerce")
        n_invalid = series.isna().sum() - df[col].isna().sum()
        n_zero = (series == 0).sum()
        n_neg = (series < 0).sum()
        print(f"  [{col}]")
        print(f"    min={series.min()}, max={series.max()}, "
              f"mean={series.mean():.2f}")
        print(f"    p50={series.quantile(0.5):.2f}, "
              f"p90={series.quantile(0.9):.2f}, "
              f"p99={series.quantile(0.99):.2f}")
        print(f"    zeros={n_zero}, negatives={n_neg}, "
              f"non-numeric={n_invalid}")
        # Top 5 values (high end) for sanity checking
        top_vals = series.nlargest(5).tolist()
        print(f"    top-5 highest: {top_vals}")


def report_categorical_cardinality(df: pd.DataFrame, columns: list[str]) -> None:
    for col in columns:
        if col not in df.columns:
            continue
        n_unique = df[col].nunique(dropna=True)
        print(f"  [{col}]: {n_unique} distinct values")
        top3 = df[col].value_counts(dropna=True).head(3)
        for val, count in top3.items():
            pct = 100 * count / len(df)
            print(f"    - '{val}': {count:,} ({pct:.1f}%)")


def report_date_range(df: pd.DataFrame, column: str) -> None:
    if column not in df.columns:
        return
    parsed = pd.to_datetime(df[column], errors="coerce")
    n_unparseable = parsed.isna().sum() - df[column].isna().sum()
    print(f"  [{column}]")
    print(f"    min date: {parsed.min()}")
    print(f"    max date: {parsed.max()}")
    print(f"    before 1880 (pre-cinema): "
          f"{(parsed < pd.Timestamp('1880-01-01')).sum()}")
    print(f"    after 2030 (suspicious future): "
          f"{(parsed > pd.Timestamp('2030-01-01')).sum()}")
    print(f"    unparseable strings: {n_unparseable}")


def report_text_quality(df: pd.DataFrame, column: str) -> None:
    if column not in df.columns:
        return
    s = df[column].dropna().astype(str)
    print(f"  [{column}]")
    print(f"    non-null entries: {len(s):,}")
    if len(s) == 0:
        return
    print(f"    avg length: {s.str.len().mean():.0f} chars")
    print(f"    min length: {s.str.len().min()}")
    print(f"    max length: {s.str.len().max()}")
    suspect_short = (s.str.len() < 10).sum()
    print(f"    suspiciously short (<10 chars): {suspect_short}")
    placeholders = s.str.lower().isin([
        "n/a", "no overview found.", "tbd", "no description",
        "no description available", "no overview", "-", "none"
    ]).sum()
    print(f"    matching known placeholders: {placeholders}")


def report_json_column(df: pd.DataFrame, column: str) -> None:
    if column not in df.columns:
        return
    sample = df[column].dropna().head(2000)
    n_ok = 0
    n_fail = 0
    n_empty_list = 0
    for v in sample:
        try:
            parsed = ast.literal_eval(v)
            n_ok += 1
            if isinstance(parsed, list) and len(parsed) == 0:
                n_empty_list += 1
        except Exception:
            n_fail += 1
    print(f"  [{column}] (sample of {len(sample)})")
    print(f"    parses OK: {n_ok}")
    print(f"    parse fails: {n_fail}")
    print(f"    empty lists: {n_empty_list}")


# =============================================================================
# Targeted checks (new in v3 — investigate dataset-specific issues)
# =============================================================================

def detect_row_shift_in_movies_metadata(df: pd.DataFrame) -> None:
    """
    Detect rows where columns appear shifted due to unescaped commas
    or newlines in the source CSV.

    Symptoms:
    - 'id' should be numeric; if it contains a date or non-numeric text,
      the row is shifted.
    - 'budget' should be numeric; same logic.
    - 'adult' should be 'True' or 'False'; anything else is shifted.
    """
    print("Checking columns that should have predictable types...")

    # id should always be convertible to int
    id_numeric = pd.to_numeric(df["id"], errors="coerce")
    non_numeric_id = id_numeric.isna().sum() - df["id"].isna().sum()
    print(f"  'id' rows with non-numeric content: {non_numeric_id}")
    if non_numeric_id > 0:
        sample = df.loc[id_numeric.isna() & df["id"].notna(), "id"].head(5).tolist()
        print(f"    examples: {sample}")

    # budget should always be numeric
    budget_numeric = pd.to_numeric(df["budget"], errors="coerce")
    non_numeric_budget = budget_numeric.isna().sum() - df["budget"].isna().sum()
    print(f"  'budget' rows with non-numeric content: {non_numeric_budget}")
    if non_numeric_budget > 0:
        sample = df.loc[budget_numeric.isna() & df["budget"].notna(),
        "budget"].head(5).tolist()
        print(f"    examples: {sample}")

    # adult should only be 'True' or 'False'
    adult_values = set(df["adult"].dropna().astype(str).unique())
    expected = {"True", "False"}
    unexpected = adult_values - expected
    print(f"  'adult' distinct values: {adult_values}")
    if unexpected:
        print(f"    unexpected values (suggesting shifted rows): {unexpected}")


def detect_text_whitespace_issues(df: pd.DataFrame, columns: list[str]) -> None:
    """
    Detect whitespace and encoding issues in text columns.

    Looks for: leading/trailing spaces, multiple consecutive spaces,
    tab/CR characters, non-breaking spaces, and Unicode escape literals.
    """
    for col in columns:
        if col not in df.columns:
            continue
        s = df[col].dropna().astype(str)
        if len(s) == 0:
            continue

        n_leading = s.str.startswith(" ").sum()
        n_trailing = s.str.endswith(" ").sum()
        n_double_space = s.str.contains(r"  +", regex=True).sum()
        n_tab = s.str.contains("\t").sum()
        n_cr = s.str.contains("\r").sum()
        n_nbsp = s.str.contains("\xa0").sum()
        n_unicode_escape = s.str.contains(r"\\u[0-9a-fA-F]{4}", regex=True).sum()

        print(f"  [{col}]")
        print(f"    leading whitespace:        {n_leading}")
        print(f"    trailing whitespace:       {n_trailing}")
        print(f"    double spaces:             {n_double_space}")
        print(f"    tab characters:            {n_tab}")
        print(f"    carriage returns:          {n_cr}")
        print(f"    non-breaking spaces:       {n_nbsp}")
        print(f"    literal Unicode escapes:   {n_unicode_escape}")


def detect_semantic_duplicates(df: pd.DataFrame) -> None:
    """
    Find duplicates by (title, release_date) — same film potentially
    uploaded twice to TMDB with different IDs.
    """
    if "title" not in df.columns or "release_date" not in df.columns:
        return
    sub = df[["title", "release_date"]].dropna()
    dups_mask = sub.duplicated(keep=False)
    n_dups = dups_mask.sum()
    print(f"  Rows with duplicate (title, release_date): {n_dups}")
    if n_dups > 0:
        print("  Examples (first 5):")
        examples = sub[dups_mask].head(5)
        for _, row in examples.iterrows():
            print(f"    - '{row['title']}' / {row['release_date']}")


def detect_empty_genres(df: pd.DataFrame) -> None:
    """
    Parse 'genres' column and count how many movies have empty list.
    """
    if "genres" not in df.columns:
        return
    n_empty = 0
    n_unparseable = 0
    for v in df["genres"].dropna():
        try:
            parsed = ast.literal_eval(v)
            if isinstance(parsed, list) and len(parsed) == 0:
                n_empty += 1
        except Exception:
            n_unparseable += 1
    n_null = df["genres"].isna().sum()
    print(f"  Movies with NULL genres:        {n_null}")
    print(f"  Movies with empty genres list:  {n_empty}")
    print(f"  Movies with unparseable genres: {n_unparseable}")
    total_without = n_null + n_empty + n_unparseable
    pct = 100 * total_without / len(df)
    print(f"  Total movies effectively without genres: "
          f"{total_without} ({pct:.1f}%)")


def compare_small_vs_full(df_small: pd.DataFrame, df_full: pd.DataFrame,
                          id_col: str, name: str) -> None:
    """
    Check whether the *_small.csv file is a strict subset of the full one,
    or has independent content.
    """
    small_ids = set(df_small[id_col])
    full_ids = set(df_full[id_col])
    overlap = small_ids & full_ids
    only_in_small = small_ids - full_ids
    only_in_full = full_ids - small_ids

    print(f"  {name}")
    print(f"    distinct IDs in small: {len(small_ids):,}")
    print(f"    distinct IDs in full:  {len(full_ids):,}")
    print(f"    overlap:               {len(overlap):,}")
    print(f"    only in small:         {len(only_in_small):,}")
    print(f"    only in full:          {len(only_in_full):,}")
    if len(only_in_small) == 0 and len(small_ids) > 0:
        print(f"    => small IS a subset of full")
    else:
        print(f"    => small is NOT a strict subset of full")


# =============================================================================
# File-specific inspections
# =============================================================================

def inspect_movies_metadata() -> None:
    section("movies_metadata.csv")
    df = pd.read_csv(DATA_RAW / "movies_metadata.csv", low_memory=False)

    subsection("Basic stats")
    report_basic_stats(df)

    subsection("Missing values")
    report_missing(df)

    subsection("Duplicates")
    report_duplicates(df, key_columns=["id", "imdb_id", "title"])

    subsection("Semantic duplicates")
    detect_semantic_duplicates(df)

    subsection("Row-shift detection")
    detect_row_shift_in_movies_metadata(df)

    subsection("Numeric anomalies")
    report_numeric_anomalies(df, ["budget", "revenue", "runtime", "popularity",
                                  "vote_average", "vote_count"])

    subsection("Categorical cardinality")
    report_categorical_cardinality(df, ["original_language", "status", "adult"])

    subsection("Date range (release_date)")
    report_date_range(df, "release_date")

    subsection("Text quality (overview)")
    report_text_quality(df, "overview")

    subsection("Text quality (tagline)")
    report_text_quality(df, "tagline")

    subsection("Whitespace and encoding issues")
    detect_text_whitespace_issues(df, ["title", "overview", "tagline"])

    subsection("JSON column validation")
    report_json_column(df, "genres")
    report_json_column(df, "production_companies")
    report_json_column(df, "spoken_languages")

    subsection("Effective genre coverage")
    detect_empty_genres(df)


def inspect_credits() -> None:
    section("credits.csv")
    df = pd.read_csv(DATA_RAW / "credits.csv", low_memory=False)

    subsection("Basic stats")
    report_basic_stats(df)

    subsection("Missing values")
    report_missing(df)

    subsection("Duplicates")
    report_duplicates(df, key_columns=["id"])

    subsection("JSON column validation")
    report_json_column(df, "cast")
    report_json_column(df, "crew")


def inspect_keywords() -> None:
    section("keywords.csv")
    df = pd.read_csv(DATA_RAW / "keywords.csv", low_memory=False)

    subsection("Basic stats")
    report_basic_stats(df)

    subsection("Missing values")
    report_missing(df)

    subsection("Duplicates")
    report_duplicates(df, key_columns=["id"])

    subsection("JSON column validation")
    report_json_column(df, "keywords")


def inspect_links() -> None:
    for filename in ["links.csv", "links_small.csv"]:
        section(filename)
        df = pd.read_csv(DATA_RAW / filename, low_memory=False)
        subsection("Basic stats")
        report_basic_stats(df)
        subsection("Missing values")
        report_missing(df)
        subsection("Duplicates")
        report_duplicates(df, key_columns=["movieId", "imdbId", "tmdbId"])

    section("links vs links_small comparison")
    df_small = pd.read_csv(DATA_RAW / "links_small.csv")
    df_full = pd.read_csv(DATA_RAW / "links.csv")
    compare_small_vs_full(df_small, df_full, "movieId", "links")


def inspect_ratings_small() -> None:
    section("ratings_small.csv")
    df = pd.read_csv(DATA_RAW / "ratings_small.csv", low_memory=False)

    subsection("Basic stats")
    report_basic_stats(df)

    subsection("Missing values")
    report_missing(df)

    subsection("Numeric anomalies")
    report_numeric_anomalies(df, ["rating", "timestamp"])

    subsection("User and movie coverage")
    print(f"  Distinct users:  {df['userId'].nunique():,}")
    print(f"  Distinct movies: {df['movieId'].nunique():,}")
    print(f"  Avg ratings per user: "
          f"{len(df) / df['userId'].nunique():.1f}")


def inspect_ratings_full() -> None:
    section("ratings.csv (large file, header + count only)")
    n_rows = sum(1 for _ in open(DATA_RAW / "ratings.csv")) - 1
    print(f"Rows: {n_rows:,}")
    head = pd.read_csv(DATA_RAW / "ratings.csv", nrows=5)
    print(f"Columns: {list(head.columns)}")
    print("(Full analysis deferred; collaborative filtering is out of scope)")


# =============================================================================
# Cross-file integrity
# =============================================================================

def cross_file_integrity() -> None:
    section("Cross-file referential integrity")

    movies = pd.read_csv(DATA_RAW / "movies_metadata.csv",
                         usecols=["id"], low_memory=False)
    credits = pd.read_csv(DATA_RAW / "credits.csv", usecols=["id"])
    keywords = pd.read_csv(DATA_RAW / "keywords.csv", usecols=["id"])

    movies["id"] = pd.to_numeric(movies["id"], errors="coerce")
    movie_ids = set(movies["id"].dropna().astype(int))
    credits_ids = set(credits["id"])
    keywords_ids = set(keywords["id"])

    print(f"Distinct movie IDs in movies_metadata: {len(movie_ids):,}")
    print(f"Distinct movie IDs in credits:         {len(credits_ids):,}")
    print(f"Distinct movie IDs in keywords:        {len(keywords_ids):,}")
    print()
    print(f"Credits orphans (id not in movies):    "
          f"{len(credits_ids - movie_ids):,}")
    print(f"Keywords orphans (id not in movies):   "
          f"{len(keywords_ids - movie_ids):,}")
    print(f"Movies without credits:                "
          f"{len(movie_ids - credits_ids):,}")
    print(f"Movies without keywords:               "
          f"{len(movie_ids - keywords_ids):,}")


# =============================================================================
# Main
# =============================================================================

def main() -> None:
    print(f"Data directory: {DATA_RAW}")
    print(f"Exists: {DATA_RAW.exists()}")
    if not DATA_RAW.exists():
        print("ERROR: data/raw/ directory not found")
        return

    inspect_movies_metadata()
    inspect_credits()
    inspect_keywords()
    inspect_links()
    inspect_ratings_small()
    inspect_ratings_full()
    cross_file_integrity()

    section("Summary")
    print("Inspection complete.")


if __name__ == "__main__":
    main()