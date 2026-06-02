# TFG — Movie analysis and recommendation platform

Final project (CFGS DAM) — Academic year 2025/2026

Web platform for exploratory analysis and personalized recommendation
of movies using semantic embeddings and vector similarity search.

## Stack

- **Database:** PostgreSQL 16 + pgvector
- **Backend:** Kotlin 2.3.0 + Ktor 3.4.3
- **Frontend:** Kotlin Compose Multiplatform (JS target, Compose HTML)
- **Data pipeline:** Python 3.12 + pandas + sentence-transformers
- **ML model:** all-mpnet-base-v2 (768-dim sentence embeddings)
- **Build system:** Gradle 9.0 (wrapper included)

## Project structure

    .
    ├── data/              # Datasets (raw CSVs + processed output)
    ├── python/            # Cleaning, transformation and embedding pipeline
    ├── server/            # Kotlin/Ktor backend (API + static file serving)
    ├── composeApp/        # Compose HTML frontend (Kotlin/JS)
    ├── shared/            # Shared DTOs between backend and frontend
    ├── migrations/        # Versioned SQL migrations (13 files)
    ├── docs/              # Memoir and figures
    ├── .run/              # IntelliJ run configurations (4 configs)
    └── .env               # Local environment variables

## Prerequisites

- **Java 21** (auto-downloaded by Gradle via Foojay toolchain resolver)
- **Python 3.12+** with [uv](https://docs.astral.sh/uv/)
- **Podman** (Linux) or **Docker** (Windows/macOS) for PostgreSQL
- **~8 GB RAM** (Kotlin/JS compiler needs 4 GB heap)

## Dataset download

The CSV files are not included in this repository due to their size.
Download them from Kaggle before running the pipeline:

1. **The Movies Dataset** (main dataset — required):
   https://www.kaggle.com/datasets/rounakbanik/the-movies-dataset

   Download and place the following files in `data/raw/`:
   - `movies_metadata.csv`
   - `credits.csv`
   - `keywords.csv`
   - `links.csv`

2. **TMDB Movies Dataset 2023** (optional — for updated poster URLs):
   https://www.kaggle.com/datasets/asaniczka/tmdb-movies-dataset-2023-930k-movies

   Download and place `TMDB_movie_dataset_v11.csv` in `data/raw/`.

## Setup — Linux

> If you received this project as a ZIP file, skip step 1.

### 1. Clone the repository

    git clone git@github.com:Sir-labdd/tfg-movies-recsys.git
    cd tfg-movies-recsys

### 2. Download datasets

Follow the instructions in **Dataset download** above.

### 3. Configure environment

    cp .env.example .env
    # Edit .env with your preferred password, or keep the defaults

> Note: if you received the ZIP with .env already included,
> this step is optional — the defaults work for local development.

### 4. Start PostgreSQL

    podman-compose up -d

Verify it is running:

    podman-compose ps

### 5. Install Python dependencies

    uv sync

### 6. Run migrations and load data

    uv run python python/scripts/07_migrate.py
    uv run python python/scripts/12_run_load_pipeline.py

### 7. Generate embeddings (~8 minutes on CPU)

    uv run python python/scripts/13_generate_embeddings.py

### 8. (Optional) Update poster URLs from newer TMDB dataset

If data/raw/TMDB_movie_dataset_v11.csv is present:

    uv run python python/scripts/14_update_poster_paths.py

### 9. Start the application

    set -a && source .env && set +a
    ./gradlew :server:runDev

Or use the IntelliJ run configurations provided in .run/.

### 10. Open the application

    http://localhost:8080

## Setup — Windows

### 1. Prerequisites

- Install **Java 21** from https://adoptium.net/ (or let Gradle download it automatically).
- Install **Python 3.12+** from https://www.python.org/downloads/.
- Install **uv** (Python package manager):

      powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"

- Install **Docker Desktop** from https://www.docker.com/products/docker-desktop/ (includes Docker Compose). Ensure WSL2 backend is enabled.

### 2. Download datasets

Follow the instructions in **Dataset download** above.

### 3. Configure environment

    copy .env.example .env

Edit .env with a text editor if needed, or keep the defaults.

### 4. Start PostgreSQL

    docker-compose up -d

Verify it is running:

    docker-compose ps

### 5. Install Python dependencies

    uv sync

### 6. Run migrations and load data

    uv run python python/scripts/07_migrate.py
    uv run python python/scripts/12_run_load_pipeline.py

### 7. Generate embeddings (~8 minutes on CPU)

    uv run python python/scripts/13_generate_embeddings.py

### 8. (Optional) Update poster URLs

If data\raw\TMDB_movie_dataset_v11.csv is present:

    uv run python python/scripts/14_update_poster_paths.py

### 9. Start the application

On Windows, environment variables must be set manually since
source .env is a Linux command. Use PowerShell:

    $env:DATABASE_JDBC_URL="jdbc:postgresql://localhost:5432/tfg_movies"
    $env:DATABASE_USER="tfg_user"
    $env:DATABASE_PASSWORD="tfg_local_dev"
    .\gradlew.bat :server:runDev

Or use the IntelliJ run configurations provided in .run/
(recommended — they load the variables automatically).

### 10. Open the application

    http://localhost:8080

## Development (hot-reload)

For frontend hot-reload during development, run backend and frontend
separately:

- **Backend:** IntelliJ -> Run "Backend (Ktor)" or ./gradlew :server:runDev
- **Frontend:** IntelliJ -> Run "Frontend (Compose JS)" or ./gradlew :composeApp:jsBrowserDevelopmentRun

The frontend dev server runs on port 8081 with hot module replacement.
API calls are proxied automatically to the backend on port 8080.

## Tests

    ./gradlew :server:test

Runs 30 integration tests against the development database (~10 seconds).
Requires PostgreSQL to be running with data loaded.

## Notes for ZIP delivery

If you received this project as a ZIP file from the author:

- The .env file is included with local development credentials.
  These are not production secrets.
- The data/raw/ directory is **not included** due to file size limits.
  Download the datasets from Kaggle following the instructions in
  **Dataset download** above.
- All build artifacts are excluded from the ZIP. Gradle and uv will
  download dependencies automatically on first run.
- The .run/ directory contains IntelliJ run configurations that
  work out of the box after opening the project.