"""
Apply pending SQL migrations to the PostgreSQL database.

Reads all .sql files in the migrations/ directory, checks which ones
have already been applied (recorded in the schema_migrations table),
and applies the pending ones in alphabetical order.

The schema_migrations table is created automatically on first run.
Each successful migration is recorded with its filename, applied_at
timestamp, and duration. This makes the runner idempotent: running it
twice in a row applies only what is missing.

Usage:
    uv run python python/scripts/07_migrate.py

Configuration: reads DATABASE_URL from the .env file at the project
root (loaded via python-dotenv if available, or falls back to environ).
"""

import os
import sys
import time
from pathlib import Path

import psycopg

from src.cleaning import setup_logging


PROJECT_ROOT = Path(__file__).resolve().parents[2]
MIGRATIONS_DIR = PROJECT_ROOT / "migrations"
ENV_FILE = PROJECT_ROOT / ".env"


def load_env_file(path: Path) -> dict[str, str]:
    """Parse a .env file into a dict. Minimal implementation: ignores
    comments (#) and empty lines, splits on first '='."""
    env: dict[str, str] = {}
    if not path.exists():
        return env
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        key, value = line.split("=", 1)
        env[key.strip()] = value.strip()
    return env


def get_database_url() -> str:
    """Return DATABASE_URL from environment, falling back to .env file."""
    url = os.environ.get("DATABASE_URL")
    if url:
        return url
    env = load_env_file(ENV_FILE)
    url = env.get("DATABASE_URL")
    if not url:
        raise RuntimeError(
            "DATABASE_URL not found in environment or .env file. "
            f"Looked for .env at {ENV_FILE}"
        )
    return url


def ensure_migrations_table(conn: psycopg.Connection) -> None:
    """Create the schema_migrations table if it doesn't exist."""
    with conn.cursor() as cur:
        cur.execute("""
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                                                                     filename     TEXT        PRIMARY KEY,
                                                                     applied_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        duration_ms  INTEGER     NOT NULL
                        )
                    """)
    conn.commit()


def get_applied_migrations(conn: psycopg.Connection) -> set[str]:
    """Return the set of filenames already applied."""
    with conn.cursor() as cur:
        cur.execute("SELECT filename FROM schema_migrations")
        return {row[0] for row in cur.fetchall()}


def discover_migration_files() -> list[Path]:
    """Return all .sql files in MIGRATIONS_DIR sorted by filename."""
    if not MIGRATIONS_DIR.exists():
        return []
    return sorted(MIGRATIONS_DIR.glob("*.sql"))


def apply_migration(
        conn: psycopg.Connection,
        migration_path: Path,
        logger,
) -> None:
    """Apply a single migration file and record it in schema_migrations."""
    sql = migration_path.read_text()
    filename = migration_path.name

    logger.info(f"Applying {filename} ...")
    start = time.time()

    with conn.cursor() as cur:
        cur.execute(sql)
        duration_ms = int((time.time() - start) * 1000)
        cur.execute(
            "INSERT INTO schema_migrations (filename, duration_ms) "
            "VALUES (%s, %s)",
            (filename, duration_ms),
        )
    conn.commit()

    logger.info(f"  OK ({duration_ms} ms)")


def main() -> None:
    logger = setup_logging()

    try:
        database_url = get_database_url()
    except RuntimeError as e:
        logger.error(str(e))
        sys.exit(1)

    # Mask password in log
    masked = database_url
    if "@" in masked and ":" in masked.split("@")[0]:
        before, after = masked.split("@", 1)
        user = before.split(":", 1)[0]
        masked = f"{user}:***@{after}"
    logger.info(f"Connecting to {masked}")

    try:
        conn = psycopg.connect(database_url)
    except psycopg.OperationalError as e:
        logger.error(f"Failed to connect: {e}")
        sys.exit(1)

    try:
        ensure_migrations_table(conn)

        all_files = discover_migration_files()
        applied = get_applied_migrations(conn)

        pending = [f for f in all_files if f.name not in applied]

        logger.info(f"Found {len(all_files)} migration files")
        logger.info(f"Already applied: {len(applied)}")
        logger.info(f"Pending: {len(pending)}")

        if not pending:
            logger.info("No pending migrations. Database is up to date.")
            return

        for migration_path in pending:
            try:
                apply_migration(conn, migration_path, logger)
            except psycopg.Error as e:
                logger.error(f"Migration {migration_path.name} FAILED: {e}")
                conn.rollback()
                sys.exit(1)

        logger.info("All pending migrations applied.")

    finally:
        conn.close()


if __name__ == "__main__":
    main()