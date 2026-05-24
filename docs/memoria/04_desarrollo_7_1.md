# TFG — Apartado 7.1 (Planificar)

> **Borrador para revisión.** Cubre la subsección **7.1 Planificar** del apartado 7 (Desarrollo) de la memoria, según el esquema oficial del centro (Planificar → Buscar → Ejecutar → Redactar).

---

## 7. Desarrollo

> *El apartado 7 constituye la sección de mayor extensión y peso evaluativo de la memoria, conforme a las directrices del centro. Se estructura en cuatro fases consecutivas: Planificar, Buscar, Ejecutar y Redactar.*

### 7.1 Planificar

#### 7.1.1 Metodología de trabajo adoptada

Dado el carácter individual del proyecto y su naturaleza claramente acotada en tiempo, se descartan tanto las metodologías clásicas en cascada (excesivamente rígidas para un proyecto donde el análisis de datos y el diseño de la base de datos se retroalimentan iterativamente) como las metodologías ágiles puras tipo Scrum (que presuponen un equipo de trabajo, ceremonias periódicas como *daily standups* o *sprint reviews*, y roles diferenciados como *Product Owner* o *Scrum Master*, ninguno de los cuales tiene sentido en un proyecto de una sola persona).

En su lugar, se adopta un **enfoque iterativo e incremental inspirado en principios ágiles pero adaptado al contexto unipersonal del proyecto**. Este enfoque presenta cuatro características definitorias.

En primer lugar, el trabajo se organiza por **bloques verticales**: cada bloque produce un incremento funcional del sistema, en lugar de completar capa por capa de forma horizontal. Por ejemplo, el bloque dedicado al sistema de recomendación incluye desde la generación de los embeddings en el pipeline de datos hasta la pantalla del frontend que muestra las recomendaciones, pasando por la migración SQL que añade la columna vectorial y el endpoint REST que devuelve los resultados. Esto permite que al final de cada bloque exista un trozo de aplicación verificable y demostrable, en lugar de tener todas las capas a medio hacer.

En segundo lugar, cada bloque se cierra con una **fase de autocrítica documentada**: tras completar un bloque, se revisa qué objetivos se han cumplido, qué problemas han aparecido y qué decisiones tomadas en bloques anteriores conviene revisar. Estas revisiones se vuelcan directamente en la memoria, lo que evita el problema clásico de redactar la documentación al final cuando ya se han olvidado las decisiones.

En tercer lugar, el código se gestiona desde el primer día mediante **control de versiones con Git**, con un repositorio único para todo el proyecto y una política de *commits* atómicos por funcionalidad o corrección. El histórico de Git constituye una fuente complementaria de información para la memoria y permite reconstruir el orden cronológico real del desarrollo en caso necesario.

En cuarto lugar, se aplica una **estrategia de priorización por riesgo**: los componentes técnicamente más inciertos (especialmente la integración con `pgvector` y la generación de embeddings) se abordan en bloques tempranos, no en bloques tardíos. Esta priorización busca evitar la situación habitual en proyectos académicos en la que un componente que se daba por sentado al inicio termina consumiendo más tiempo del previsto en la recta final.

#### 7.1.2 Planificación temporal

El proyecto se desarrolla en un periodo aproximado de dos semanas, comprendido entre finales de mayo y mediados de junio. La planificación se organiza en nueve bloques de trabajo de duración variable, con dependencias claras entre ellos y con posibilidad de solapamiento parcial en aquellos casos donde dos tareas se retroalimentan (por ejemplo, el análisis del dataset informa al diseño de la base de datos, por lo que ambos bloques se solapan parcialmente).

A continuación se detalla cada bloque junto con su duración estimada y sus dependencias.

| ID | Bloque | Días | Dependencias |
|----|--------|------|--------------|
| B1 | Definición del proyecto y revisión bibliográfica | 1 | — |
| B2 | Análisis del dataset y diagnóstico de calidad | 2 | B1 |
| B3 | Diseño de la base de datos | 1 | B2 |
| B4 | Pipeline de limpieza y carga inicial | 2 | B2, B3 |
| B5 | Implementación del backend (capa REST) | 2 | B3, B4 |
| B6 | Implementación del frontend (pantallas base) | 3 | B5 |
| B7 | Modelo predictivo de valoración | 1 | B4 |
| B8 | Sistema de recomendación con embeddings | 2 | B4 |
| B9 | Integración, pruebas y redacción final de la memoria | 2 | B5-B8 |

La duración total estimada, considerando los solapamientos previstos, se sitúa en torno a quince días naturales de trabajo. La planificación incluye además un margen de aproximadamente tres días para contingencias, dado que la experiencia previa en proyectos de complejidad similar indica que entre el 15% y el 20% del tiempo total se consume en imprevistos no contemplados en la planificación inicial.

El diagrama de Gantt correspondiente a esta planificación se incluye como **Figura 1** a continuación.

![Diagrama de Gantt del proyecto](../img/gantt.png)
*Figura 1. Diagrama de Gantt con la planificación de los nueve bloques de trabajo.*

#### 7.1.3 Estrategia de priorización por riesgo

Tal como se ha indicado, los bloques técnicamente más inciertos se abordan en posiciones tempranas o intermedias de la planificación, no en posiciones finales. Concretamente, los tres riesgos principales identificados *a priori* son los siguientes.

**Riesgo 1: configuración e integración de pgvector**. La extensión pgvector no forma parte de la instalación por defecto de PostgreSQL y su comportamiento en operaciones de carga masiva no ha sido validado previamente por el autor. Por este motivo, la primera operación que se realiza al inicio del bloque B5 (implementación del backend) es una carga sintética de prueba en una tabla con columna `vector`, con el objetivo de validar la cadena completa (instalación → migración → inserción → consulta por similitud) antes de invertir tiempo en otros componentes.

**Riesgo 2: tiempo de generación de los embeddings**. La generación de embeddings para aproximadamente cuarenta y cinco mil sinopsis mediante un modelo ejecutado en CPU puede consumir un tiempo no despreciable. Se contempla por tanto la posibilidad de trabajar inicialmente con un subconjunto de mil películas seleccionadas (por ejemplo, las más populares según la métrica `popularity` del dataset original) para iterar rápidamente sobre el sistema de recomendación, y diferir la generación masiva de embeddings al final del proyecto, una vez validada la cadena completa.

**Riesgo 3: ajuste de Compose Multiplatform para el target JavaScript**. El soporte de Compose Multiplatform para *Web* sigue evolucionando y las versiones recientes presentan diferencias en su sistema de empaquetado que pueden requerir ajustes específicos. Para mitigar este riesgo, el bloque B5 (backend) incluye desde el primer día una pantalla mínima en el frontend que verifica la comunicación cliente-servidor, garantizando que el camino crítico está abierto antes de iniciar el bloque B6, dedicado a las pantallas funcionales.
