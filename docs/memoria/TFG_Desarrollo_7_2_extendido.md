# TFG — Apartado 7.2 (Buscar): versión extendida con resultados reales

> **Borrador de la memoria.** Sustituye la versión anterior del apartado 7.2.
> Mantiene el tono académico impersonal y aporta las cifras reales obtenidas
> mediante el script de inspección, sustituyendo todas las estimaciones
> previas.

---

## 7.2 Buscar

### 7.2.1 Búsqueda y selección del dataset

El primer paso operativo del proyecto, una vez establecidos los objetivos y la arquitectura general, consistió en la búsqueda y selección de un dataset adecuado a los requisitos del trabajo. Los criterios de selección establecidos fueron los siguientes: licencia compatible con uso académico (preferentemente CC0 o equivalente, que minimiza las restricciones legales y administrativas), volumen suficiente para justificar técnicas de aprendizaje automático (al menos varios miles de registros), estructura relacional natural que permitiera modelar varias entidades con sus relaciones (en lugar de una única tabla plana), riqueza de información que justificara una webapp con múltiples vistas (en lugar de un dashboard de modelo único), y presencia de contenido textual que permitiera aplicar técnicas de procesamiento del lenguaje natural y generación de embeddings semánticos.

Se evaluaron varias opciones representativas de distintos dominios. Las opciones consideradas y descartadas fueron las siguientes.

**Home Credit Default Risk** (Kaggle): dataset de predicción de impago de préstamos con siete tablas interconectadas. Se descartó debido a que su licencia es la genérica de competiciones de Kaggle, cuyos términos exactos requerían verificación específica y no garantizaban con claridad la libertad de redistribución necesaria para un proyecto académico.

**European Soccer Database** (Kaggle): base de datos relacional en formato SQLite con información de partidos, jugadores y equipos de las principales ligas europeas. Licencia ODbL compatible con uso académico. Se descartó debido a que el dataset finaliza en la temporada 2015/2016, lo que podía resultar desactualizado.

**Credit Card Fraud Detection** (Kaggle, MLG-ULB): dataset clásico de detección de fraude con clases altamente desbalanceadas. Licencia limpia (DbCL/CC0). Se descartó por dos motivos principales: la totalidad de sus variables numéricas son resultado de una transformación PCA sobre variables originales anonimizadas, lo que impide cualquier interpretación humana de las mismas; y la inexistencia de entidades con identidad propia, lo que dificulta la construcción de una webapp con vistas tipo listado-detalle.

Finalmente se seleccionó **The Movies Dataset** (autor: Rounak Banik, plataforma: Kaggle, licencia: CC0 1.0 Dominio Público) por cumplir simultáneamente todos los criterios establecidos: licencia plenamente libre, volumen significativo, estructura multi-tabla, riqueza temática que sustenta tanto la webapp como la memoria, y presencia de contenido textual rico en el campo `overview` (sinopsis) sobre el que aplicar técnicas de embeddings semánticos.

### 7.2.2 Estructura del dataset seleccionado

El dataset seleccionado se organiza en siete archivos CSV. A continuación se describen únicamente los cinco que entran dentro del alcance del proyecto. El archivo `ratings.csv` (26.024.289 filas) y su versión reducida `ratings_small.csv` quedan fuera del alcance, dado que el sistema de filtrado colaborativo está declarado como línea de trabajo futuro.

| Archivo | Filas | Columnas | Función |
|---|---|---|---|
| `movies_metadata.csv` | 45.466 | 24 | Entidad central: una fila por película con sus metadatos |
| `credits.csv` | 45.476 | 3 | Equipo artístico (`cast`) y técnico (`crew`) por película |
| `keywords.csv` | 46.419 | 2 | Palabras clave temáticas por película |
| `links.csv` | 45.843 | 3 | Equivalencias entre identificadores de TMDB, IMDb y MovieLens |
| `links_small.csv` | 9.125 | 3 | Versión reducida de `links.csv` |

Las cifras de filas en `credits.csv` y `keywords.csv` no coinciden exactamente con las de `movies_metadata.csv` debido a la presencia de duplicados internos (estudiada en apartados posteriores). El análisis de integridad referencial cruzada (apartado 7.2.5) confirma que los identificadores cruzan correctamente entre archivos una vez deduplicados.

### 7.2.3 Metodología de inspección

Con el objetivo de obtener un diagnóstico fiable y reproducible de la calidad del dataset, se diseñó un script de inspección automatizado en Python que se ejecuta sobre los archivos CSV originales sin modificarlos. El script (recogido íntegramente en el repositorio del proyecto bajo la ruta `python/scripts/01_inspect_raw.py`) aplica tres niveles de comprobación sobre cada archivo.

**El primer nivel, denominado *known-issue checks*, consiste en verificaciones específicas sobre problemas que se sospecha que existen en el dataset.** Estas comprobaciones derivan de documentación previa sobre el dataset y de la experiencia general de trabajar con datasets de origen abierto. Ejemplos incluyen la detección de valores cero encubiertos en variables económicas, la detección de duplicados por identificador y la validación de rangos plausibles para fechas.

**El segundo nivel, denominado *broad sweep*, aplica genéricamente sobre todas las columnas un conjunto de estadísticas descriptivas estándar**: tipos de datos detectados, valores ausentes por columna, percentiles para columnas numéricas, cardinalidad para columnas categóricas, longitudes para columnas de texto. Este nivel busca poner de manifiesto problemas que no se habían anticipado.

**El tercer nivel, denominado *targeted checks*, aplica comprobaciones específicas dirigidas a anomalías conocidas en este dataset concreto.** Incluye la detección de filas desplazadas mediante coherencia de tipos entre columnas, el análisis de caracteres no imprimibles y espacios anómalos en columnas de texto, la detección de duplicados semánticos por combinación de campos, y la validación de los campos anidados en formato JSON.

El script se diseñó respetando dos principios. Primero, **reproducibilidad estricta**: cualquier persona que clone el repositorio, descargue el dataset y ejecute el script obtendrá exactamente las mismas cifras documentadas en esta memoria. Segundo, **no destructividad**: el script únicamente lee los archivos originales y produce salida por consola; no modifica ningún dato.

La ejecución completa del script sobre el dataset original se realizó en mayo de 2026 y tardó aproximadamente dos minutos en una máquina de desarrollo estándar. Los resultados que se recogen en los siguientes apartados se derivan directamente de esa ejecución.

### 7.2.4 Resultados de la inspección por archivo

A continuación se recogen los hallazgos más relevantes derivados del informe del script. La salida íntegra del script se incluye como anexo de la memoria para consulta detallada. En este apartado se recogen únicamente los fragmentos que justifican decisiones de diseño posteriores.

#### Sobre `movies_metadata.csv`

Es el archivo central del dataset y el que presenta mayor variedad de problemas de calidad. Sus 24 columnas se reparten entre identificadores (2), atributos numéricos (6), atributos categóricos (4), una fecha, atributos de texto libre (5) y campos con estructuras anidadas en formato JSON (6).

La inspección de valores ausentes confirma cuatro columnas con porcentajes significativos:

| Columna | Ausentes | Porcentaje |
|---|---|---|
| `belongs_to_collection` | 40.972 | 90,1% |
| `homepage` | 37.684 | 82,9% |
| `tagline` | 25.054 | 55,1% |
| `overview` | 954 | 2,1% |

Los altos porcentajes en `belongs_to_collection` y `homepage` son esperables: solo un subconjunto reducido de películas forma parte de una franquicia identificada en TMDB, y solo las producciones grandes tienen página oficial registrada. Estos porcentajes no constituyen un problema a resolver, sino una característica del dataset que se debe asumir.

Por el contrario, los **954 valores ausentes en `overview`** sí constituyen un problema relevante para el proyecto, dado que la sinopsis es el campo sobre el que se calculan los embeddings semánticos del sistema de recomendación. Las películas sin sinopsis no podrán recibir ni emitir recomendaciones basadas en contenido, lo que obliga a definir explícitamente una política de tratamiento (apartado 7.2.5).

#### Sobre `credits.csv` y `keywords.csv`

Ambos archivos presentan una estructura muy simple (identificador de película más uno o dos campos JSON) y no contienen valores ausentes. Sin embargo, ambos archivos contienen un volumen significativo de **duplicados completos** que conviene resolver al cargar:

- `credits.csv`: 37 filas duplicadas en su totalidad, afectando a 87 filas en relaciones de duplicación.
- `keywords.csv`: **987 filas duplicadas en su totalidad**, afectando a 1.972 filas. Este volumen es desproporcionadamente alto y resulta uno de los hallazgos más llamativos de la inspección.

La presencia de duplicados completos sugiere que el dataset fue generado o agregado a partir de fuentes solapadas sin un proceso de deduplicación posterior. La estrategia de tratamiento (apartado 7.2.5) consiste en una deduplicación trivial al cargar.

#### Sobre `links.csv` y `links_small.csv`

Ambos archivos comparten esquema (`movieId`, `imdbId`, `tmdbId`). El análisis comparativo del solapamiento entre los dos archivos arroja un resultado inesperado:

| Conjunto | Cardinalidad |
|---|---|
| IDs en `links_small.csv` | 9.125 |
| IDs en `links.csv` | 45.843 |
| IDs en ambos | 9.115 |
| Solo en `links_small.csv` | 10 |
| Solo en `links.csv` | 36.728 |

La existencia de 10 identificadores presentes únicamente en la versión reducida demuestra que `links_small.csv` **no es un subconjunto estricto de `links.csv`** sino un archivo generado con criterios parcialmente independientes. Esta observación lleva a la decisión de utilizar exclusivamente `links.csv` en el proyecto y descartar la versión reducida, evitando la duplicidad y la complejidad de tratar las inconsistencias.

#### Sobre la integridad referencial cruzada entre archivos

La inspección incluyó una comprobación específica de integridad referencial entre los tres archivos principales (`movies_metadata.csv`, `credits.csv` y `keywords.csv`). El resultado es notablemente positivo:

| Indicador | Valor |
|---|---|
| Identificadores distintos en `movies_metadata` | 45.433 |
| Identificadores distintos en `credits` | 45.432 |
| Identificadores distintos en `keywords` | 45.432 |
| Identificadores en `credits` huérfanos (no aparecen en `movies_metadata`) | 0 |
| Identificadores en `keywords` huérfanos (no aparecen en `movies_metadata`) | 0 |
| Películas en `movies_metadata` sin entrada en `credits` | 1 |
| Películas en `movies_metadata` sin entrada en `keywords` | 1 |

Esta práctica ausencia de huérfanos es la mejor noticia de la inspección de cara al modelado relacional: las relaciones entre tablas pueden establecerse mediante claves foráneas estrictas sin necesidad de tratamientos especiales para casos de identificadores no encontrados.

### 7.2.5 Catálogo de problemas identificados

Esta subsección recoge sistemáticamente los problemas detectados durante la inspección. Para cada uno se documenta su síntoma observado, su diagnóstico técnico, su impacto sobre el proyecto y la estrategia de tratamiento adoptada.

#### P1 — Filas desplazadas en `movies_metadata.csv`

**Síntoma.** El script detecta tres filas en las que la columna `id`, que debería contener un identificador numérico, contiene en su lugar valores tipo `'1997-08-20'`, `'2012-09-29'` y `'2014-01-01'`. Las mismas tres filas presentan en `budget` valores tipo `'/ff9qCepilowshEtG2GYWwzt2bs4.jpg'`, y en `adult` aparecen fragmentos de texto que claramente pertenecen al campo `overview` (sinopsis), incluyendo cadenas como `' - Written by Ørnås'` o `' Avalanche Sharks tells the story of a bikini contest...'`.

**Diagnóstico.** El patrón de los valores observados indica un desplazamiento horizontal del contenido en estas tres filas. Probablemente fueron originadas por la presencia de comas o saltos de línea no escapados correctamente en algún campo de texto del CSV original, lo que provocó que el parser interpretara incorrectamente los límites entre columnas. Es un problema conocido en datasets CSV con texto libre.

**Impacto.** Si no se tratan, estas filas contaminarían todos los análisis numéricos posteriores: la conversión de `budget` a `float` fallaría, las estadísticas de `revenue` quedarían sesgadas, y los modelos predictivos recibirían entrada corrupta.

**Estrategia de tratamiento.** Eliminación completa de las tres filas durante la primera fase del pipeline de limpieza, identificándolas mediante el criterio inequívoco de que `id` no es convertible a entero. Dado que representan apenas el 0,007% del dataset, su eliminación no introduce sesgo apreciable.

#### P2 — Valores cero encubiertos en variables económicas

**Síntoma.** El script reporta 36.573 películas (80,4%) con `budget == 0` y 38.052 películas (83,7%) con `revenue == 0`.

**Diagnóstico.** Inspección manual de una muestra confirma que estos ceros no representan presupuestos o recaudaciones reales nulas, sino la ausencia del dato en la fuente original. La distinción es crítica para cualquier análisis estadístico: tratar estos ceros como valores reales conduciría a una distorsión severa de las distribuciones.

**Impacto.** Afecta directamente al modelo predictivo de `vote_average`, dado que `budget` constituye uno de los predictores potencialmente más informativos. Tratar el cero como valor real introduciría sesgo de gran magnitud.

**Estrategia de tratamiento.** Sustitución de los ceros por valores ausentes (`NaN`) durante la limpieza. La aplicación web mostrará en la ficha de cada película un texto del tipo "Presupuesto no disponible" cuando se trate de uno de estos casos. El modelo predictivo se entrenará exclusivamente sobre las películas que disponen del dato real.

#### P3 — Valores cero encubiertos en variables de valoración

**Síntoma.** El script reporta 2.998 películas con `vote_average == 0` y 2.899 películas con `vote_count == 0`. La cuasi-coincidencia entre ambas cifras sugiere fuerte correlación.

**Diagnóstico.** Una valoración media de cero solo es interpretable si efectivamente nadie ha votado: no significa "la película es muy mala" sino "no hay votos suficientes". Por lo tanto, las películas con `vote_count == 0` deben tener su `vote_average` tratado también como ausente, no como una valoración real de cero.

**Impacto.** Si se ignora este patrón, el modelo predictivo "aprenderá" la regla espúrea de que las películas sin votos tienen rating cero, lo que destruye su capacidad de predicción.

**Estrategia de tratamiento.** Se aplica una regla condicional durante la limpieza: cuando `vote_count == 0`, se asigna `NaN` también a `vote_average`. Las películas resultantes quedan disponibles en el catálogo pero excluidas del entrenamiento del modelo predictivo.

#### P4 — Valores anómalos en `runtime`

**Síntoma.** El script reporta 1.558 películas con `runtime == 0`, así como valores en el extremo superior que claramente no corresponden a películas convencionales: 1.256 minutos (21 horas), 1.140 minutos, 931 minutos y otros valores superiores a 900 minutos.

**Diagnóstico.** Los valores de cero corresponden a entradas sin duración registrada (ceros encubiertos, análogos a P2 y P3). Los valores extremadamente altos corresponden probablemente a contenido erróneamente catalogado: maratones, recopilaciones de series, o errores tipográficos en la fuente.

**Impacto.** Limitado pero apreciable: `runtime` es uno de los predictores del modelo y su distribución se vería distorsionada por estos outliers.

**Estrategia de tratamiento.** Doble criterio durante la limpieza: cuando `runtime == 0`, se asigna `NaN`; cuando `runtime > 300`, se asigna `NaN` por considerar el valor implausible para una película convencional. El umbral de 300 minutos es deliberadamente generoso (cinco horas) para no excluir películas largas legítimas como las epopeyas históricas.

#### P5 — Películas sin géneros asignados

**Síntoma.** El script reporta 2.442 películas (5,4% del dataset) cuya columna `genres`, aunque parsea correctamente como JSON, contiene una lista vacía.

**Diagnóstico.** Estas películas existen en el dataset pero carecen de clasificación temática. No es un error de calidad del CSV sino una ausencia genuina de información en la fuente.

**Impacto.** Una webapp orientada a la exploración por género debería contemplar este caso explícitamente. Excluir estas películas reduciría el catálogo en más de dos mil títulos sin justificación clara.

**Estrategia de tratamiento.** Las películas sin género se conservan en el catálogo y se les asigna una etiqueta virtual "Sin clasificar" (no presente en TMDB como categoría real, pero creada en el proceso de limpieza). Esto permite mostrarlas en la webapp y filtrarlas si el usuario lo desea.

#### P6 — Anomalías de espaciado en columnas de texto

**Síntoma.** El script detecta en la columna `overview` un total de 1.348 entradas con dobles espacios consecutivos, 5 con espacios al inicio, 5 con espacios al final, y 17 con caracteres *non-breaking space* (`\xa0`). Patrones similares aparecen en `tagline` y `title`.

**Diagnóstico.** El texto en estas columnas no está normalizado. Estas anomalías son invisibles para un lector humano pero alteran las representaciones vectoriales generadas por el modelo de embeddings, dado que dos cadenas que difieren únicamente en su espaciado producirán vectores ligeramente distintos.

**Impacto.** Crítico para el sistema de recomendación basado en embeddings: dos películas con sinopsis textualmente idéntica pero con distinto espaciado serían tratadas como similares pero no idénticas, lo que degrada sutilmente la calidad de las recomendaciones.

**Estrategia de tratamiento.** Normalización del texto previa al cálculo de embeddings: aplicación de `strip()` para eliminar espacios al inicio y final, colapsado de espacios consecutivos a un único espacio, y reemplazo de *non-breaking spaces* por espacios normales.

#### P7 — Sinopsis con texto placeholder

**Síntoma.** El script identifica 141 entradas en `overview` cuyo contenido coincide con cadenas conocidas como `"No overview found."`, `"N/A"`, `"TBD"` y similares.

**Diagnóstico.** Estas sinopsis no contienen información real sobre la película; son marcadores que indican la ausencia de la descripción. Tratarlas como sinopsis legítimas para el cálculo de embeddings generaría un "embedding placeholder" compartido por todas estas películas, que serían erróneamente consideradas mutuamente similares por el sistema de recomendación.

**Impacto.** Si no se trata, el sistema de recomendación devolvería entre las películas "similares" un conjunto recurrente formado por todas las películas sin sinopsis real, lo que sería visiblemente anómalo en la webapp.

**Estrategia de tratamiento.** Las 141 entradas se sustituyen por `NaN` durante la limpieza, equiparando su tratamiento al de las 954 entradas que ya estaban vacías originalmente.

#### P8 — Duplicados completos en `credits.csv` y `keywords.csv`

**Síntoma.** `credits.csv` contiene 37 filas duplicadas en su totalidad y `keywords.csv` contiene 987.

**Diagnóstico.** Filas idénticas (mismo identificador y mismo contenido) sin justificación aparente. Sugiere una agregación de fuentes solapadas durante la generación del dataset original.

**Impacto.** Sin tratamiento, la carga a la base de datos generaría errores de violación de clave primaria (en caso de definir `id` como clave única) o duplicaría información (en caso contrario).

**Estrategia de tratamiento.** Deduplicación trivial al cargar mediante `drop_duplicates()`, conservando la primera ocurrencia de cada fila.

#### P9 — Duplicados por identificador en `movies_metadata.csv`

**Síntoma.** El script reporta 79 filas involucradas en relaciones de duplicación por `imdb_id` y 59 por la combinación (`title`, `release_date`).

**Diagnóstico.** Al contrario que P8, aquí los duplicados no son idénticos sino que tienen distintos identificadores TMDB pero apuntan a la misma película (mismo identificador IMDb, o mismo título y fecha). Probablemente se trata de versiones distintas de la misma entrada subidas por colaboradores diferentes a TMDB.

**Impacto.** Afecta al diseño de la BD si se decide deduplicar (la película aparecería una sola vez en el catálogo) o no (aparecería dos o más veces). La decisión condiciona la integridad referencial con `credits.csv` y `keywords.csv`.

**Estrategia de tratamiento.** Deduplicación conservando la primera ocurrencia por `imdb_id`. Las filas eliminadas tendrán sus correspondientes entradas en `credits` y `keywords` igualmente descartadas si no se referencian, garantizando la consistencia entre tablas.

#### P10 — Fechas anómalas en `release_date`

**Síntoma.** El script reporta 2 películas con fecha de estreno anterior a 1880, año previo a la invención del cine como medio.

**Diagnóstico.** La fecha más antigua reportada es `1874-12-09`. Aunque esta fecha precede a la consolidación del cine como medio comercial, sí coincide con experimentos cinematográficos pioneros documentados. No puede descartarse que correspondan a entradas legítimas del dataset sobre proto-cine.

**Impacto.** Mínimo. Dos filas en un dataset de 45.466 no condicionan ningún análisis agregado.

**Estrategia de tratamiento.** Las fechas se conservan sin modificación. En la webapp se mostrarán con la fecha original. No se intenta validar caso por caso dado el volumen reducido y la posibilidad de que sean entradas legítimas.

### 7.2.6 Decisiones de diseño consolidadas

La fase de búsqueda ha proporcionado tanto el diagnóstico de calidad detallado como el conjunto de decisiones que orientarán las fases siguientes. Las decisiones consolidadas son las siguientes.

**Sobre el modelado de la base de datos**, se confirma la viabilidad de un esquema relacional normalizado con `movies` como entidad central. La integridad referencial impecable entre archivos (cero huérfanos en `credits` y `keywords`) permite definir claves foráneas estrictas sin necesidad de tolerar valores no encontrados.

**Sobre el pipeline de limpieza**, se establece la secuencia operativa siguiente: (1) saneamiento estructural eliminando las tres filas desplazadas y casteando los tipos de datos; (2) marcado como ausentes de los valores cero encubiertos en `budget`, `revenue`, `vote_average` (condicional a `vote_count`) y `runtime`; (3) normalización del texto en columnas de sinopsis y eslogan; (4) deduplicación de los tres archivos principales; (5) tratamiento de placeholders en `overview`; (6) etiquetado especial de las películas sin género.

**Sobre el modelo predictivo**, se confirma que el entrenamiento se restringirá a las películas que disponen de los datos económicos y de votación reales (no encubiertos). El catálogo visible en la webapp se mantendrá completo, separando así el alcance del modelado del alcance del producto visible.

**Sobre el sistema de recomendación**, se confirma la necesidad de normalizar el texto antes del cálculo de embeddings y de excluir las películas con sinopsis ausente o placeholder. Las películas sin sinopsis quedarán visibles en la webapp pero no aparecerán como candidatas en las recomendaciones, lo que se documentará explícitamente en la interfaz.

**Sobre los archivos a cargar**, se confirma el uso de `movies_metadata.csv`, `credits.csv`, `keywords.csv` y `links.csv`. Se descarta `links_small.csv` por no constituir un subconjunto estricto del completo, y se mantiene fuera del alcance `ratings.csv` por estar el filtrado colaborativo declarado como línea de trabajo futuro.

La fase **7.3 Ejecutar** se desarrolla en las subsecciones siguientes, implementando los componentes del sistema en el orden previsto por la planificación temporal y aplicando las decisiones de diseño aquí consolidadas.
