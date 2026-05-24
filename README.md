# TFG — Plataforma de análisis y recomendación de películas

Trabajo de Fin de Ciclo (CFGS DAM) — Curso 2025/2026

Plataforma web para el análisis exploratorio y la recomendación personalizada
de películas mediante aprendizaje automático y embeddings semánticos.

## Stack

- **Base de datos:** PostgreSQL 16 + pgvector
- **Backend:** Kotlin + Ktor
- **Frontend:** Kotlin Compose Multiplatform (target JS)
- **Pipeline de datos:** Python 3.12 + pandas + scikit-learn + sentence-transformers

## Estructura

\`\`\`
.
├── data/              # Datos (no versionados, ver instrucciones de descarga)
├── python/            # Pipeline de limpieza, transformación y ML
├── server/            # Backend Kotlin/Ktor
├── composeApp/        # Frontend Compose Multiplatform JS
├── shared/            # Modelos compartidos entre backend y frontend
├── migrations/        # Migraciones SQL versionadas
└── docs/              # Memoria y figuras
\`\`\`

## Reproducir el proyecto

(Se completará conforme avance el desarrollo.)
