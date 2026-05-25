"""
Orchestrator: runs the four cleaning scripts in order and reports
progress.

This script does not contain cleaning logic. Each individual cleaning
script remains independently executable; this orchestrator simply runs
them in sequence and times each stage for reporting.

Usage:
    uv run python python/scripts/06_run_pipeline.py
"""

import subprocess
import sys
import time
from pathlib import Path

from src.cleaning import setup_logging


PROJECT_ROOT = Path(__file__).resolve().parents[2]
SCRIPTS_DIR = PROJECT_ROOT / "python" / "scripts"

PIPELINE_STEPS = [
    "02_clean_movies.py",
    "03_clean_credits.py",
    "04_clean_keywords.py",
    "05_clean_links.py",
]


def run_step(script_name: str, logger) -> tuple[bool, float]:
    """Run a single pipeline script and return (success, elapsed_seconds)."""
    script_path = SCRIPTS_DIR / script_name

    if not script_path.exists():
        logger.error(f"Script not found: {script_path}")
        return False, 0.0

    logger.info(f"=== Running {script_name} ===")
    start = time.time()

    # Use the same Python interpreter that's running the orchestrator.
    # This guarantees the venv environment is preserved.
    result = subprocess.run(
        [sys.executable, str(script_path)],
        capture_output=False,  # Let child output flow to our stdout
    )

    elapsed = time.time() - start
    success = result.returncode == 0

    if success:
        logger.info(f"=== {script_name} completed in {elapsed:.1f}s ===")
    else:
        logger.error(
            f"=== {script_name} FAILED (exit code {result.returncode}) "
            f"after {elapsed:.1f}s ==="
        )

    return success, elapsed


def main() -> None:
    logger = setup_logging()
    logger.info(f"Pipeline starting: {len(PIPELINE_STEPS)} stages")

    pipeline_start = time.time()
    results: list[tuple[str, bool, float]] = []

    for script_name in PIPELINE_STEPS:
        success, elapsed = run_step(script_name, logger)
        results.append((script_name, success, elapsed))

        if not success:
            logger.error("Pipeline aborted due to failure")
            break

    total_elapsed = time.time() - pipeline_start

    # Summary
    logger.info("=" * 60)
    logger.info("PIPELINE SUMMARY")
    logger.info("=" * 60)
    for script, success, elapsed in results:
        status = "OK " if success else "FAIL"
        logger.info(f"  [{status}] {script:30s} {elapsed:6.1f}s")
    logger.info(f"Total elapsed: {total_elapsed:.1f}s")

    # Exit with non-zero if any step failed
    all_ok = all(s for _, s, _ in results) and len(results) == len(PIPELINE_STEPS)
    sys.exit(0 if all_ok else 1)


if __name__ == "__main__":
    main()