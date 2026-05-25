"""
Generic utilities for loading processed data into the PostgreSQL
database.

Provides:
- Database connection management consistent with 07_migrate.py.
- Bulk loading via COPY FROM STDIN (PostgreSQL's native fast path).
- Safe parsing of Python-literal-encoded JSON columns from the CSVs.
- Helper to track row counts for logging.

This module follows the same conventions as src/cleaning.py: pure
functions, structured logging, type hints.
"""

import ast
import csv
import io
import os
from pathlib import Path
from typing import Any, Iterable, Sequence

import psycopg


PROJECT_ROOT = Path(__file__).resolve().parents[2]
ENV_FILE = PROJECT_ROOT / ".env"
DATA_PROCESSED = PROJECT_ROOT / "data" / "processed"


# =============================================================================
# Environment and connection
# =============================================================================

def _load_env_file(path: Path) -> dict[str, str]:
    """Minimal .env parser. Same logic as in 07_migrate.py."""
    env: dict[str, str] = {}
    if not path.exists():
        return env
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        env[key.strip()] = value.strip()
    return env


def get_database_url() -> str:
    """Return DATABASE_URL from environment or .env file."""
    url = os.environ.get("DATABASE_URL")
    if url:
        return url
    env = _load_env_file(ENV_FILE)
    url = env.get("DATABASE_URL")
    if not url:
        raise RuntimeError(
            f"DATABASE_URL not found in environment or .env file at {ENV_FILE}"
        )
    return url


def connect() -> psycopg.Connection:
    """Open a connection to the database using DATABASE_URL."""
    return psycopg.connect(get_database_url())


# =============================================================================
# Python-literal parsing
# =============================================================================

def parse_python_literal(value: Any) -> Any:
    """
    Parse a CSV cell containing a Python literal (typically a list of
    dicts or a single dict).

    The Movies Dataset stores nested structures as Python's repr()
    output, not strict JSON, so we use ast.literal_eval instead of
    json.loads. Returns None for NaN/empty inputs.
    """
    if value is None:
        return None
    # pandas reads empty cells as NaN (float); guard against it
    if isinstance(value, float):
        return None
    s = str(value).strip()
    if not s or s.lower() in ("nan", "none"):
        return None
    try:
        return ast.literal_eval(s)
    except (ValueError, SyntaxError):
        return None


# =============================================================================
# Bulk loading via COPY FROM STDIN
# =============================================================================

def copy_rows(
        conn: psycopg.Connection,
        table: str,
        columns: Sequence[str],
        rows: Iterable[Sequence[Any]],
) -> int:
    """
    Bulk-load rows into the given table via COPY FROM STDIN.

    This is PostgreSQL's native fast path: typically 50-100x faster
    than individual INSERT statements. The 'rows' iterable can be a
    list, a generator, or any iterable yielding tuples/lists of values
    in the same order as 'columns'.

    None values are correctly converted to SQL NULL.

    Returns the total number of rows copied.
    """
    column_list = ", ".join(columns)
    sql = f"COPY {table} ({column_list}) FROM STDIN WITH (FORMAT CSV)"

    # Build an in-memory CSV buffer that psycopg's COPY consumes
    buffer = io.StringIO()
    writer = csv.writer(buffer, quoting=csv.QUOTE_MINIMAL)
    n_rows = 0
    for row in rows:
        # Convert None to empty string (CSV NULL representation when no NULL
        # token is configured); for safer null handling we'll set the NULL
        # token explicitly below via WITH (NULL '\\N')
        writer.writerow([_serialize_value(v) for v in row])
        n_rows += 1

    buffer.seek(0)

    # Use explicit NULL token so empty strings stay empty strings and
    # None values become SQL NULL.
    sql_with_null = (
        f"COPY {table} ({column_list}) FROM STDIN "
        f"WITH (FORMAT CSV, NULL '\\N')"
    )

    with conn.cursor() as cur:
        with cur.copy(sql_with_null) as copy:
            copy.write(buffer.getvalue())

    return n_rows


def _serialize_value(v: Any) -> str:
    """
    Convert a Python value to the string form expected inside the
    in-memory CSV used by COPY FROM STDIN.

    None becomes the explicit NULL token '\\N'; everything else is
    str()'d. Booleans become 't'/'f' which PostgreSQL accepts.
    """
    if v is None:
        return "\\N"
    if isinstance(v, bool):
        return "t" if v else "f"
    if isinstance(v, float):
        # pandas can yield float NaN even after dropna; treat as NULL
        import math
        if math.isnan(v):
            return "\\N"
        return repr(v)
    return str(v)


# =============================================================================
# Upsert helper for canonical entities deduplicated during load
# =============================================================================

def upsert_canonical_entities(
        conn: psycopg.Connection,
        table: str,
        id_column: str,
        columns: Sequence[str],
        rows: Iterable[Sequence[Any]],
) -> int:
    """
    Insert rows into a canonical entity table, ignoring conflicts on the
    primary key.

    Used when iterating over all movies extracts the same entity
    (e.g. genre 'Drama') hundreds of times. With ON CONFLICT DO NOTHING,
    only the first occurrence is inserted; subsequent duplicates are
    silently skipped.

    Returns the number of rows actually inserted (not skipped).
    """
    column_list = ", ".join(columns)
    placeholders = ", ".join(["%s"] * len(columns))

    sql = (
        f"INSERT INTO {table} ({column_list}) "
        f"VALUES ({placeholders}) "
        f"ON CONFLICT ({id_column}) DO NOTHING"
    )

    inserted = 0
    with conn.cursor() as cur:
        for row in rows:
            cur.execute(sql, row)
            if cur.rowcount > 0:
                inserted += 1

    return inserted