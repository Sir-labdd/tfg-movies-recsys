# TFG — Apartado 7.3 (Ejecutar): Limpieza y transformación de datos

> **Borrador para revisión.** Primera subsección del apartado 7.3 ("Ejecutar"). Cubre la implementación del pipeline de limpieza que transforma los archivos CSV originales del dataset en versiones procesadas listas para su carga en la base de datos, aplicando las estrategias de tratamiento definidas en el apartado 7.2.5.

---

## 7.3.1 Limpieza y transformación de datos

> *El apartado 7.2 concluyó con un catálogo de diez problemas de calidad identificados en el dataset original (P1 a P10) y las correspondientes estrategias de tratamiento para cada uno. Esta subsección documenta la implementación de dichas estrategias como un pipeline de scripts Python automatizado, reproducible e idempotente.*

### 7.3.1.1 Planificación

La limpieza de datos constituye los bloques B1 y B2 de la planificación temporal expuesta en el apartado 7.1. La descomposición sigue la estructura natural del dataset: un script por cada archivo CSV que requiere tratamiento, más un orquestador que los ejecuta en secuencia.

**Sub-paso B1 — Inspección automatizada.** Implementar el script de inspección (`01_inspect_raw.py`) documentado en el apartado 7.2.3, que genera el diagnóstico completo de calidad sobre los archivos CSV originales. Este script no modifica datos; su función es producir el informe que fundamenta las decisiones de limpieza.

**Sub-paso B2.1 — Limpieza de `movies_metadata.csv`.** Aplicar las estrategias P1 (filas desplazadas), P2 (ceros encubiertos en variables económicas), P3 (ceros encubiertos en valoración), P4 (runtime anómalo), P6 (espaciado), P7 (sinopsis placeholder) y P9 (duplicados por identificador), produciendo `data/processed/movies.csv`.

**Sub-paso B2.2 — Limpieza de `credits.csv`.** Aplicar la estrategia P8 (duplicados completos) y deduplicación adicional por identificador, produciendo `data/processed/credits.csv`.

**Sub-paso B2.3 — Limpieza de `keywords.csv`.** Aplicar la estrategia P8 (duplicados completos, 987 filas), produciendo `data/processed/keywords.csv`.

**Sub-paso B2.4 — Limpieza de `links.csv`.** Eliminar filas sin identificador TMDB, convertir tipos y deduplicar por identificador, produciendo `data/processed/links.csv`.

**Sub-paso B2.5 — Orquestación del pipeline.** Implementar un script orquestador (`06_run_pipeline.py`) que ejecuta los cuatro scripts de limpieza en secuencia, reporta el tiempo de cada fase y aborta la ejecución si alguno falla.

### 7.3.1.2 Investigación

La investigación técnica previa a la implementación se centró en dos aspectos: la arquitectura del pipeline y la elección de herramientas.

**Arquitectura del pipeline.** Se optó por una arquitectura de scripts independientes orquestados externamente, frente a la alternativa de un único script monolítico. Cada script de limpieza lee un archivo CSV original de `data/raw/`, aplica sus transformaciones y escribe el resultado en `data/processed/`. Esta independencia tiene tres ventajas: permite ejecutar cualquier paso de forma aislada durante el desarrollo y la depuración, facilita la adición futura de nuevos archivos sin modificar los existentes, y produce una salida intermedia inspeccionable para cada paso (los archivos procesados pueden verificarse manualmente antes de cargar a la base de datos).

**Herramientas.** Se seleccionó **pandas** como biblioteca de manipulación de datos por ser el estándar de facto para procesamiento de CSV en Python y por ofrecer las operaciones necesarias (filtrado, casting, deduplicación, manipulación de texto) de forma expresiva y eficiente para datasets de este tamaño (decenas de miles de filas). Se descartaron alternativas como Polars (más rápida en datasets de millones de filas, pero innecesaria aquí y con una API menos documentada en el contexto académico) y procesamiento línea a línea con la biblioteca estándar `csv` (demasiado verboso para las transformaciones requeridas).

Las funciones de limpieza reutilizables se centralizaron en un módulo `python/src/cleaning.py` para evitar duplicación entre scripts. Este módulo expone funciones puras que reciben un DataFrame, lo transforman y devuelven un DataFrame nuevo, siguiendo el patrón de pipeline funcional. Cada función incluye logging del número de filas afectadas antes y después de la transformación, lo que permite auditar el impacto de cada paso sin inspeccionar manualmente los archivos de salida.

### 7.3.1.3 Ejecución

**Módulo de funciones de limpieza (`python/src/cleaning.py`).** Se implementaron diecisiete funciones de limpieza reutilizables, organizadas por categoría: saneamiento estructural (`drop_shifted_rows`, `cast_types`, `parse_release_date`), tratamiento de valores encubiertos (`zeros_to_nan`, `vote_average_to_nan_when_no_votes`, `clip_runtime`, `mask_suspicious_budget`, `mask_unreliable_ratings`), normalización de texto (`normalize_whitespace`, `replace_placeholders_with_nan`, `mask_short_overviews`), deduplicación (`drop_full_duplicates`, `drop_duplicates_by_key`), filtrado (`filter_adult`), y tratamiento de estado (`fill_status_unknown`). Cada función registra mediante el logger el número de filas modificadas, eliminadas o marcadas como ausentes.

**Script de limpieza de películas (`02_clean_movies.py`).** Es el script más complejo del pipeline por la variedad de problemas que debe tratar. La secuencia de transformaciones sigue un orden estricto: primero se eliminan las tres filas desplazadas (P1), después se convierten los tipos de datos, se parsea la fecha de estreno, se reemplazan los ceros encubiertos por valores ausentes (P2, P3, P4), se normalizan los textos (P6), se eliminan los placeholders de sinopsis (P7), se marcan los presupuestos sospechosos (valores redondos improbables), se filtran las películas adultas, y finalmente se deduplican por identificador (P9). El orden no es arbitrario: la eliminación de filas desplazadas debe preceder al casting de tipos (las filas desplazadas causarían errores de conversión), y la deduplicación debe ser posterior a la normalización de texto (para que las comparaciones sean coherentes).

**Scripts de limpieza de `credits.csv`, `keywords.csv` y `links.csv`.** Los tres scripts son significativamente más simples que el de películas. La limpieza de `credits.csv` aplica dos pases de deduplicación: uno por filas completas (P8, 37 duplicados) y otro por identificador (11 filas adicionales con mismo ID pero contenido distinto). La limpieza de `keywords.csv` aplica un único pase de deduplicación completa (P8, 987 duplicados). La limpieza de `links.csv` elimina filas sin identificador TMDB, convierte `tmdbId` de float a entero, y deduplica por identificador TMDB para garantizar la restricción de unicidad requerida por la clave foránea en la base de datos.

**Orquestador (`06_run_pipeline.py`).** El orquestador ejecuta los cuatro scripts de limpieza en secuencia mediante `subprocess.run`, usando el mismo intérprete Python que lo ejecuta (garantizando que se respeta el entorno virtual). Si cualquier script falla (código de salida distinto de cero), el pipeline aborta inmediatamente. Al finalizar, imprime un resumen con el nombre de cada script, su estado (OK/FAIL) y su duración. La ejecución completa del pipeline sobre el dataset original se completó en aproximadamente tres segundos.

### 7.3.1.4 Dificultades encontradas

El pipeline de limpieza se ejecutó sin dificultades técnicas significativas. El análisis exhaustivo realizado en la fase de inspección (apartado 7.2) identificó los problemas con suficiente antelación como para que las estrategias de tratamiento pudieran implementarse de forma directa.

**Auditoría secundaria.** Durante la implementación del script de películas se descubrieron cinco problemas adicionales no detectados en la inspección inicial (denominados P11 a P17 en los comentarios del código): duplicados por identificador en `credits.csv` con contenido discordante (P11), duplicados por `tmdbId` en `links.csv` (P12), presupuestos con valores sospechosamente redondos (P13), películas con cero votos pero valoración distinta de cero (P14), y sinopsis excesivamente cortas que no aportan valor semántico (P15). Estos problemas se integraron en el pipeline sin alterar su estructura, añadiendo funciones de limpieza adicionales al módulo `cleaning.py`. La auditoría secundaria confirma el valor del enfoque iterativo: una primera pasada identifica los problemas evidentes, la implementación revela los sutiles.

**Decisión de conservar el catálogo completo.** Una tentación recurrente durante la limpieza fue la de eliminar agresivamente las películas con datos incompletos (sin sinopsis, sin fecha, sin género). Se optó deliberadamente por la estrategia contraria: conservar todas las películas en el catálogo visible de la webapp, marcando los campos ausentes como `NaN` para que la interfaz los trate caso por caso (mostrando "No disponible" o excluyendo la película de funcionalidades específicas como las recomendaciones). Esta decisión maximiza la utilidad del catálogo para el usuario sin comprometer la integridad de los análisis.
