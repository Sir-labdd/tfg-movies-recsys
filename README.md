# TFG — Movie analysis and recommendation platform

Final project (CFGS DAM) — Academic year 2025/2026

Web platform for exploratory analysis and personalized recommendation
of movies using semantic embeddings and vector similarity search.

## Stack

- **Database:** PostgreSQL 16 + pgvector
- **Backend:** Kotlin + Ktor
- **Frontend:** Kotlin Compose Multiplatform (JS target, Compose HTML)
- **Data pipeline:** Python 3.12 + pandas + sentence-transformers
- **ML model:** all-mpnet-base-v2 (768-dim sentence embeddings)

## Structure

    .
    ├── data/              # Datasets (not versioned, see setup instructions)
    ├── python/            # Cleaning, transformation and embedding pipeline
    ├── server/            # Kotlin/Ktor backend (API + static file serving)
    ├── composeApp/        # Compose HTML frontend (Kotlin/JS)
    ├── shared/            # Shared DTOs between backend and frontend
    ├── migrations/        # Versioned SQL migrations
    ├── docs/              # Memoir and figures
    └── .run/              # IntelliJ run configurations

## Prerequisites

- Java 21 (auto-downloaded by Gradle via Foojay toolchain resolver)
- Python 3.12+ with [uv](https://docs.astral.sh/uv/)
- Podman or Docker (for PostgreSQL container)
- ~8 GB RAM (Kotlin/JS compiler needs 4 GB heap)

## Setup

1. Clone the repository and configure environment:

        git clone git@github.com:Sir-labdd/tfg-movies-recsys.git
        cd tfg-movies-recsys
        cp .env.example .env

2. Start PostgreSQL:

        podman-compose up -d

3. Install Python dependencies:

        uv sync

4. Run migrations and load data:

        uv run python python/scripts/07_migrate.py
        uv run python python/scripts/12_run_load_pipeline.py

5. Generate embeddings (~8 min on CPU):

        uv run python python/scripts/13_generate_embeddings.py

6. Start the application:

        ./gradlew :server:runDev

   Or use the IntelliJ run configurations in the `.run/` directory.

7. Open http://localhost:8080

## Development

For frontend hot-reload during development, run backend and frontend
separately:

- **Backend:** IntelliJ → Run "Backend (Ktor)" or `./gradlew :server:runDev`
- **Frontend:** IntelliJ → Run "Frontend (Compose JS)" or `./gradlew :composeApp:jsBrowserDevelopmentRun`

The frontend dev server runs on port 8081 and proxies API calls to
the backend on port 8080.

## Tests

    ./gradlew :server:test

Runs 30 integration tests against the development database (~10 seconds).