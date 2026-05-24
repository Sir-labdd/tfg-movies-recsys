# TFG — Bloque 1: Definición del proyecto

> **Borrador para revisión.** Este documento cubre los apartados de **Justificación** (sección 4 de la memoria), **Objetivos** (sección 6) y **Alcance** (que se ubicará dentro del apartado 7 "Desarrollo"). Está redactado en tono académico formal e impersonal, en infinitivo donde corresponde, y respeta las indicaciones de la guía oficial del centro.

---

## Título provisional

**Diseño e implementación de una plataforma web para el análisis y recomendación de películas mediante aprendizaje automático y embeddings semánticos.**

---

## 4. Justificación del proyecto

El presente Trabajo Fin de Grado nace de la confluencia de tres factores: la experiencia profesional adquirida durante el periodo de prácticas curriculares en empresa, el interés personal hacia las tecnologías emergentes basadas en inteligencia artificial, y la voluntad de consolidar de forma integrada los conocimientos adquiridos a lo largo del Ciclo Formativo de Grado Superior en Desarrollo de Aplicaciones Multiplataforma.

Durante el periodo de formación en centros de trabajo, el alumno ha participado en el desarrollo de un sistema de análisis y visualización de datos sobre empresas tecnológicas de la aceleradora Y Combinator, lo que le ha permitido familiarizarse con un stack tecnológico moderno basado en Kotlin para el lenguaje principal, Ktor para el desarrollo backend, Compose Multiplatform para el frontend web, y PostgreSQL como sistema gestor de bases de datos relacional. Este conjunto de competencias se ha aplicado, sin embargo, a un dominio muy específico y dentro del marco operativo de la empresa, sin que el alumno haya tenido la oportunidad de abordar el ciclo de desarrollo completo de forma autónoma ni de explorar técnicas más avanzadas asociadas a la inteligencia artificial moderna.

El presente proyecto persigue trasladar y consolidar ese conjunto de competencias técnicas a un dominio diferente —el análisis y la recomendación de productos cinematográficos— incorporando además una capa adicional de complejidad tecnológica mediante la integración de modelos de lenguaje y técnicas de búsqueda vectorial basadas en embeddings semánticos. La elección del dominio cinematográfico responde a la disponibilidad de un dataset público de calidad contrastada (*The Movies Dataset* publicado en Kaggle bajo licencia CC0), a su atractivo visual para una aplicación interactiva, y a la riqueza de información estructurada que permite plantear simultáneamente tareas de exploración, predicción y recomendación.

La integración de embeddings semánticos resulta especialmente relevante dado el creciente protagonismo de estas tecnologías en el panorama actual del desarrollo de software, donde la búsqueda por similitud vectorial se ha convertido en un componente habitual de sistemas de recomendación, asistentes conversacionales y motores de búsqueda inteligente. Su incorporación al proyecto constituye además una preparación natural para los estudios de máster especializados en inteligencia artificial que el alumno tiene previsto cursar tras la finalización del ciclo formativo.

En definitiva, el TFG se concibe como un puente entre el conocimiento adquirido durante el ciclo, la experiencia práctica desarrollada en la empresa de prácticas, y la formación especializada futura, materializado en una aplicación web completa de extremo a extremo que demuestra el dominio integrado de las competencias propias del título.

---

## 6. Objetivos

### Objetivo general

Desarrollar una plataforma web para el análisis exploratorio y la recomendación personalizada de películas, aplicando técnicas de aprendizaje automático y embeddings semánticos sobre un dataset público de información cinematográfica.

### Objetivos específicos

1. **Diseñar e implementar un pipeline de limpieza y transformación de datos** que resuelva de forma documentada los problemas de calidad presentes en el dataset original, incluyendo la normalización de campos anidados en formato JSON, el tratamiento de valores ausentes y la corrección de inconsistencias temporales.

2. **Modelar una base de datos relacional** que represente adecuadamente las entidades del dominio (películas, personas, géneros, compañías productoras) junto con sus relaciones, aplicando los principios de normalización y diseñando los índices necesarios para garantizar un rendimiento adecuado de las consultas.

3. **Desarrollar un servidor backend** con arquitectura por capas (rutas, repositorios, modelos) que exponga los datos del sistema mediante una interfaz REST documentada, garantizando separación de responsabilidades y facilidad de mantenimiento.

4. **Implementar una aplicación web cliente** que ofrezca al usuario al menos cinco vistas funcionales (listado con filtros, ficha de detalle de película, ficha de persona, panel analítico, vista de recomendaciones) con una experiencia de usuario coherente y un diseño visual cuidado.

5. **Entrenar y evaluar un modelo predictivo** capaz de estimar la valoración media de una película (`vote_average`) a partir de atributos disponibles con anterioridad a su estreno, aplicando metodología estándar de validación cruzada y reportando métricas de error apropiadas.

6. **Implementar un sistema de recomendación basado en embeddings semánticos** extraídos a partir de las sinopsis de las películas, almacenando dichos embeddings en una base de datos vectorial y devolviendo recomendaciones por similitud coseno en tiempo razonable.

7. **Integrar los modelos desarrollados dentro de la aplicación web** de forma transparente al usuario final, ofreciendo tanto la valoración estimada como las recomendaciones de películas similares dentro del flujo de navegación natural.

8. **Documentar de forma completa el proyecto** mediante una memoria técnica que recoja las decisiones de diseño adoptadas, los problemas encontrados durante el desarrollo y las soluciones aplicadas a los mismos.

---

## Alcance del proyecto

> *Este apartado no constituye una sección independiente de la memoria pero se incluye aquí como referencia interna y se integrará dentro del apartado 7 "Desarrollo".*

### Qué entra en el alcance del proyecto

El proyecto contempla el desarrollo completo de los siguientes componentes:

- **Pipeline de datos**, implementado en Python 3.12 mediante scripts ejecutables que toman como entrada los archivos CSV originales del dataset y producen como salida tanto archivos limpios como cargas directas a la base de datos.

- **Base de datos relacional** en PostgreSQL con extensión `pgvector` activada, gobernada mediante un sistema de migraciones SQL versionadas, que incluye al menos las tablas para películas, personas, vínculos cast/crew, géneros, compañías productoras y embeddings.

- **Backend** en Kotlin con framework Ktor, organizado en capas (`routes`, `repositories`, `models`), que expone una API REST con al menos seis endpoints distintos cubriendo listado de películas con filtros, detalle de película, listado de personas, detalle de persona, predicción de valoración y recomendación por similitud.

- **Aplicación web cliente** en Compose Multiplatform compilada a JavaScript, con al menos cinco pantallas funcionales, navegación interna entre ellas, llamadas asíncronas al backend, y un diseño visual coherente.

- **Modelo de regresión** entrenado en Python con scikit-learn para predecir la valoración media de una película, persistido para su consulta desde el backend.

- **Generador de embeddings** que procesa las sinopsis del dataset mediante un modelo preentrenado (`sentence-transformers` o equivalente), almacenando los vectores resultantes en la base de datos.

- **Memoria técnica** en formato PDF de entre 20 y 40 páginas, redactada conforme a la guía oficial del centro.

### Qué queda fuera del alcance del proyecto

Se excluyen explícitamente los siguientes elementos, que podrán plantearse como líneas de trabajo futuro:

- **Autenticación de usuarios**. La plataforma no contempla sistema de registro, login ni gestión de cuentas. Todas las funcionalidades son accesibles públicamente sin sesión.

- **Despliegue en producción**. El sistema se entrega como aplicación desplegable en entorno local mediante contenedores. No se incluye configuración de HTTPS, dominio propio, servidor de producción ni mecanismos de CI/CD.

- **Panel de administración**. No se desarrolla una interfaz administrativa para la gestión del contenido. Toda alteración del catálogo se realiza fuera de banda mediante scripts.

- **Internacionalización**. La interfaz se desarrolla únicamente en castellano. La memoria, sin embargo, incluye un *abstract* en inglés conforme a las indicaciones del centro.

- **Aplicación móvil nativa**. La interfaz se desarrolla exclusivamente para navegador web. La capacidad multiplataforma del framework Compose se aprovecha únicamente para la versión JavaScript.

- **Sistema de recomendación basado en filtrado colaborativo**. Aunque el dataset incluye un archivo de valoraciones de usuarios apto para esta técnica, su implementación queda fuera del alcance debido a las limitaciones temporales del proyecto. Se contempla como línea de trabajo futuro.

- **Recomendaciones personalizadas por usuario**. Dado que la plataforma no gestiona cuentas, las recomendaciones se calculan exclusivamente a partir del contenido de una película de referencia (recomendación basada en contenido), no a partir de un historial de usuario.

### Limitaciones conocidas a priori

- **Calidad parcial del dataset**: el dataset original presenta una proporción significativa de campos con valores ausentes o aparentes (presupuestos y recaudaciones con valor cero que en realidad significan "desconocido"), lo que limita la capacidad predictiva de los modelos. Estas limitaciones se documentarán y se asumirán como parte del análisis.

- **Capacidad predictiva del modelo de regresión**: la predicción de la valoración de una película es un problema notoriamente complejo en la literatura, dado que depende en gran medida de factores no presentes en los datos (marketing, contexto del estreno, calidad de las críticas posteriores). Las métricas reportadas se interpretarán dentro de este contexto.

- **Recursos computacionales**: el proyecto se desarrolla y evalúa en entorno local sin GPU. La generación de embeddings se realizará en CPU o, si fuera necesario, mediante servicios externos de API. Esta limitación se reflejará en los tiempos de cómputo reportados.
