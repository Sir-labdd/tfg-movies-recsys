# TFG — Movie analysis and recommendation platform

Final project (CFGS DAM) — Academic year 2025/2026

Web platform for exploratory analysis and personalized recommendation
of movies using machine learning and semantic embeddings.

## Stack

- **Database:** PostgreSQL 16 + pgvector
- **Backend:** Kotlin + Ktor
- **Frontend:** Kotlin Compose Multiplatform (JS target)
- **Data pipeline:** Python 3.12 + pandas + scikit-learn + sentence-transformers

## Structure

\`\`\`
.
├── data/              # Datasets (not versioned, see download instructions)
├── python/            # Cleaning, transformation and ML pipeline
├── server/            # Kotlin/Ktor backend
├── composeApp/        # Compose Multiplatform JS frontend
├── shared/            # Shared models between backend and frontend
├── migrations/        # Versioned SQL migrations
└── docs/              # Memoir and figures
\`\`\`

## Reproducing the project

(To be completed as development progresses.)