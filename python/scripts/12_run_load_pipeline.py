"""
Orchestrator: runs the four data-loading scripts in order and reports
progress.

Replica of 06_run_pipeline.py adapted for the loading stage. Each
loading script is independent and can be executed standalone; this
orchestrator only coordinates and times them.

Usage:
    uv run python python/scripts/12_run_load_pipeline.py

Pre-requisites:
- PostgreSQL container running (podman-compose up -d).
- All migrations applied (07_migrate.py).
- Processed CSVs present in data/processed/.
"""

import subprocess
import sys
import time
from pathlib import Path

from src.cleaning import setup_logging


PROJECT_ROOT = Path(__file__).resolve().parents[2]
SCRIPTS_DIR = PROJECT_ROOT / "python" / "scripts"

PIPELINE_STEPS = [
    "08_load_movies.py",
    "09_load_categories.py",
    "10_load_credits.py",
    "11_load_links.py",
]


def run_step(script_name: str, logger) -> tuple[bool, float]:
    script_path = SCRIPTS_DIR / script_name
    if not script_path.exists():
        logger.error(f"Script not found: {script_path}")
        return False, 0.0

    logger.info(f"=== Running {script_name} ===")
    start = time.time()
    result = subprocess.run(
        [sys.executable, str(script_path)],
        capture_output=False,
    )
    elapsed = time.time() - start
    success = result.returncode == 0
    if success:
        logger.info(f"=== {script_name} completed in {elapsed:.1f}s ===")
    else:
        logger.error(
            f"=== {script_name} FAILED (exit {result.returncode}) "
            f"after {elapsed:.1f}s ==="
        )
    return success, elapsed


def main() -> None:
    logger = setup_logging()
    logger.info(f"Load pipeline starting: {len(PIPELINE_STEPS)} stages")

    pipeline_start = time.time()
    results: list[tuple[str, bool, float]] = []

    for script_name in PIPELINE_STEPS:
        ok, elapsed = run_step(script_name, logger)
        results.append((script_name, ok, elapsed))
        if not ok:
            logger.error("Load pipeline aborted due to failure")
            break

    total = time.time() - pipeline_start
    logger.info("=" * 60)
    logger.info("LOAD PIPELINE SUMMARY")
    logger.info("=" * 60)
    for script, ok, elapsed in results:
        status = "OK " if ok else "FAIL"
        logger.info(f"  [{status}] {script:30s} {elapsed:6.1f}s")
    logger.info(f"Total elapsed: {total:.1f}s")

    all_ok = all(s for _, s, _ in results) and len(results) == len(PIPELINE_STEPS)
    sys.exit(0 if all_ok else 1)


if __name__ == "__main__":
    main()