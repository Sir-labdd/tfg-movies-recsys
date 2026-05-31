# TFG — Sección 8: Conclusiones y ampliaciones futuras

> **Borrador para revisión.** Sección final de la memoria antes de la bibliografía. Recoge las conclusiones del proyecto, el grado de cumplimiento de los objetivos, las lecciones aprendidas durante el desarrollo y las líneas de ampliación identificadas.

---

## 8. Conclusiones

### 8.1 Grado de cumplimiento de los objetivos

Los objetivos definidos en la sección 6 de esta memoria se plantearon en tres niveles: el objetivo general, los objetivos específicos y los objetivos de aprendizaje. A continuación se evalúa honestamente el grado de cumplimiento de cada uno.

**Objetivo general: diseñar y construir una plataforma web funcional que permita explorar un catálogo de películas y recibir recomendaciones personalizadas basadas en similitud semántica.** El objetivo se considera cumplido. El sistema entregado permite al usuario explorar un catálogo de más de cuarenta y cinco mil películas con filtros por género, ordenación y búsqueda textual, consultar el detalle de cada película con su reparto y metadatos, y recibir recomendaciones de películas similares calculadas mediante embeddings semánticos de un modelo Transformer pre-entrenado. La plataforma se ejecuta como un proceso único que sirve tanto la API como la interfaz web desde un solo puerto, facilitando su despliegue y demostración.

**Objetivos específicos.** Se desglosan a continuación:

El objetivo de construir un pipeline de datos automatizado y reproducible para la adquisición, limpieza y carga de un dataset real se cumple: los trece scripts Python ejecutan la cadena completa desde los CSV originales hasta la base de datos poblada, incluyendo la generación de embeddings, sin intervención manual más allá de la configuración inicial.

El objetivo de diseñar un esquema relacional normalizado que modele las entidades del dominio y sus relaciones se cumple: el esquema PostgreSQL sigue la tercera forma normal con tablas de unión para las relaciones muchos-a-muchos, versionado mediante trece migraciones SQL idempotentes gestionadas por un runner propio.

El objetivo de implementar un sistema de recomendación basado en contenido mediante embeddings semánticos se cumple: el modelo all-mpnet-base-v2 genera vectores de 768 dimensiones para cuarenta y cuatro mil películas, almacenados en PostgreSQL con pgvector e indexados con HNSW, con tiempos de consulta de aproximadamente dieciséis milisegundos tras la primera petición.

El objetivo de exponer los datos y las recomendaciones mediante una API HTTP documentada se cumple: el backend Ktor expone cinco endpoints (listado paginado con filtros, detalle, búsqueda, recomendaciones similares y listado de géneros) con respuestas tipadas, manejo de errores estructurado y una suite de treinta pruebas de integración.

El objetivo de construir una interfaz web que consuma la API y ofrezca una experiencia de exploración visual se cumple: la aplicación Compose HTML implementa listado con scroll infinito, detalle con recomendaciones, búsqueda con autocompletado en tiempo real y alternancia de tema claro/oscuro.

**Objetivos de aprendizaje.** El proyecto ha permitido adquirir experiencia práctica en tecnologías no cubiertas por el currículo del ciclo: Kotlin Multiplatform (compilación a JVM y JavaScript desde el mismo código fuente), programación declarativa de interfaces (Compose), bases de datos vectoriales (pgvector, índices HNSW), modelos de procesamiento del lenguaje natural (sentence-transformers), y gestión de proyectos multi-módulo con Gradle. Estos aprendizajes se documentan a lo largo de la sección 7 con las decisiones técnicas y dificultades que los motivaron.

### 8.2 Lecciones aprendidas

El desarrollo del proyecto ha producido aprendizajes que trascienden las tecnologías específicas utilizadas y que merecen documentarse por su valor formativo.

**El cuello de botella del aprendizaje automático basado en contenido es el contenido, no el modelo.** La iteración sobre la elección del modelo de embeddings (documentada en la subsección 7.3.4.4) demostró que la calidad de las recomendaciones depende más de la riqueza del texto fuente que de la sofisticación del modelo. Las recomendaciones para películas cuya sinopsis describe adecuadamente sus temas fueron excelentes; las recomendaciones para películas con sinopsis literales o escuetas fueron mediocres independientemente del modelo utilizado. Esta observación tiene implicaciones prácticas directas: antes de invertir en modelos más complejos, conviene invertir en enriquecer los datos de entrada.

**Las herramientas generan fricción cuando se combinan de formas no previstas por sus autores.** La combinación de Kotlin Multiplatform, Compose para la Web, Ktor y Gradle reveló incompatibilidades no documentadas en cada punto de contacto: accessors deprecados silenciosamente entre versiones menores, tareas Gradle que no propagan variables de entorno al proceso forked, compiladores que agotan la memoria con dependencias que individualmente son ligeras. La resolución de estos problemas consumió más tiempo del previsto y rara vez produjo conocimiento transferible (la solución a un conflicto de versiones entre Kotlin 2.3.0 y Ktor 3.4.3 no aplica a ninguna otra combinación). La lección es presupuestar tiempo de integración explícitamente al combinar tecnologías de ecosistemas distintos.

**Los datos estáticos caducan.** El uso de un dataset fijo de TMDB como fuente de datos reveló que las URLs de pósteres almacenadas en el dataset original habían expirado parcialmente: TMDB actualiza y elimina imágenes de su CDN de forma periódica. La solución técnica (un fallback visual en el frontend) fue sencilla, pero la observación tiene implicaciones de diseño: cualquier sistema que almacene referencias a recursos externos debe prever su invalidación. Una ampliación natural del proyecto consistiría en actualizar periódicamente los datos desde la API de TMDB.

**La documentación honesta de las dificultades es más valiosa que la documentación de los éxitos.** Las secciones de dificultades de cada subsección de la memoria (7.3.1.4 a 7.3.5.4) resultaron ser las más largas y las más interesantes de escribir. Documentar lo que no funcionó a la primera, las iteraciones necesarias y las soluciones de compromiso adoptadas produce un texto más útil para un lector futuro que la mera enumeración de las soluciones finales.

### 8.3 Ampliaciones futuras

El proyecto deja identificadas varias líneas de ampliación que, estando fuera del alcance temporal del trabajo, constituyen extensiones naturales del sistema construido.

**Pantalla de persona (actor/director) con filmografía.** La base de datos contiene las tablas `people`, `movie_cast` y `movie_crew` con los créditos completos de cada película. Un nuevo endpoint `GET /people/{id}` que devolviera la información de una persona junto con su filmografía, y una pantalla correspondiente en el frontend, permitiría al usuario navegar el catálogo desde la perspectiva del talento creativo además de desde la perspectiva de la película. La arquitectura actual soporta esta ampliación sin modificaciones estructurales: el endpoint seguiría el mismo patrón Repository/Service/Routes del backend, y la pantalla seguiría el patrón screens/components del frontend.

**Filtrado colaborativo.** El dataset original incluye un archivo `ratings.csv` con más de veintiséis millones de valoraciones de usuarios de MovieLens, declarado fuera del alcance en la fase de planificación. La incorporación de estos datos permitiría implementar un sistema de recomendación colaborativo (basado en las preferencias de usuarios similares) que complementara al sistema de recomendación basado en contenido ya implementado. Un sistema híbrido que combinara ambas aproximaciones produciría recomendaciones de mayor calidad, especialmente para películas cuya sinopsis es insuficiente para capturar su atractivo (el caso de Fight Club documentado en 7.3.4.4).

**Actualización periódica del dataset.** El uso de un dataset estático introduce dos limitaciones: las películas estrenadas después de la fecha del dataset no aparecen en el catálogo, y las URLs de pósteres pueden expirar. Una ampliación natural consistiría en implementar un proceso de actualización periódica que consultara la API pública de TMDB (con clave de acceso propia), descargara las películas nuevas y actualizara las existentes. El pipeline de limpieza y carga ya implementado está diseñado para ser idempotente, lo que facilitaría la incorporación de actualizaciones incrementales.

**Despliegue en la nube.** El sistema actual está diseñado para ejecución local. Un despliegue en un servicio de nube (por ejemplo, un contenedor Docker en un servicio como Fly.io o Railway) requeriría: empaquetar el servidor como un JAR ejecutable (la tarea `buildFatJar` del plugin Ktor ya lo soporta), configurar una base de datos PostgreSQL gestionada con pgvector (disponible en Supabase, Neon o Amazon RDS), y ajustar las variables de entorno para apuntar a la base de datos remota. La arquitectura de proceso único (backend + frontend estático) simplifica este despliegue al requerir un solo contenedor.

**Autenticación y listas personales.** La incorporación de un sistema de autenticación (por ejemplo, mediante Firebase Auth como en el proyecto referente) permitiría almacenar listas personales de películas favoritas, un historial de películas consultadas y preferencias de filtrado. Estas funcionalidades transformarían la plataforma de una herramienta de exploración anónima a una herramienta personalizada, lo que a su vez enriquecería el sistema de recomendación con señales de preferencia explícitas del usuario.
