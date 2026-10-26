# Proyecto: App de empleo accesible con IA

23/10/2026 · Angela

## Resumen

Vamos a construir una app web que ayuda a personas con discapacidad intelectual a encontrar empleo privado, con todo en lectura fácil y con pictogramas. Es un proyecto de portfolio para demostrar que sé construir con IA generativa, agentes y skills.

La app cubre lo que hoy no cubre nadie: las apps existentes se centran en oposiciones, y esta se centra en empresa privada, Centros Especiales de Empleo (CEE) y empleo con apoyo.

**Usuarios:**

- **La persona que busca empleo**: tiene discapacidad intelectual, a menudo con certificado igual o superior al 33%.
- **El preparador laboral**: profesional de empleo con apoyo que acompaña a la persona y revisa todo antes de enviarlo.

**Qué hace la app, en una frase por módulo:**

1. Crea el perfil de la persona con una entrevista guiada en lectura fácil.
2. Genera un CV doble: versión ATS para empresas y versión en lectura fácil para la persona.
3. Busca ofertas y las filtra por empresas que contratan a personas con certificado del 33% o más.
4. Escribe cartas de presentación adaptadas a cada oferta.
5. Prepara la entrevista de trabajo.

**Sobre mí como autora:** vengo del sector social, en concreto de discapacidad intelectual. Sé programar en PHP, Java, Python y TypeScript. Quiero ir poco a poco para aprender cada pieza.

## Instrucciones para el asistente de código

Este proyecto es para aprender, no solo para tener la app hecha. Si eres un asistente de IA (por ejemplo Claude Code) trabajando conmigo, sigue estas reglas:

1. **Una fase cada vez.** No avances a la siguiente fase hasta que yo diga que la anterior está terminada y entendida.
2. **Pasos pequeños.** Divide cada fase en tareas de 30 a 60 minutos. Al empezar una fase, propón el plan de tareas y espera mi visto bueno.
3. **Explica antes de escribir.** Antes de cada bloque de código, explícame qué vamos a hacer y por qué. Si aparece un concepto nuevo (embeddings, tool use, MCP, etc.), explícalo en 3 o 4 frases con un ejemplo.
4. **Déjame escribir partes a mí.** En cada tarea, deja al menos una parte para que la programe yo, con pistas si me atasco. Revisa mi código y dime qué mejorarías.
5. **No metas dependencias sin explicarlas.** Cada librería nueva: para qué sirve y qué alternativas había.
6. **Tests y commits pequeños.** Cada tarea termina con un test que pasa y un commit con mensaje claro.
7. **Diario de aprendizaje.** Al cerrar cada fase, ayúdame a actualizar `APRENDIZAJE.md` con lo que aprendí, los problemas que tuve y cómo los resolví.
8. **Pregunta si dudas.** Si algo de este documento no está claro, pregunta antes de inventar.
9. **Idioma.** Habla en español. El código y los nombres de variables, en inglés. La interfaz de la app, en español y en lectura fácil.

## Contexto del dominio

**Lectura fácil.** Método para que los textos sean comprensibles para personas con dificultades de comprensión. En España se recoge en la norma UNE 153101 EX. Pautas básicas que usaremos:

- Frases cortas, una idea por frase.
- Vocabulario sencillo y frecuente. Explicar las palabras difíciles.
- Sin abreviaturas, siglas sin explicar, porcentajes ni números romanos.
- Voz activa y orden sujeto, verbo, complemento.
- Un solo tema por párrafo, con apoyo de imágenes.
- Los textos se validan con personas lectoras con discapacidad intelectual.

**ARASAAC.** Banco público y gratuito de pictogramas del Gobierno de Aragón, con API abierta. Licencia Creative Commons BY-NC-SA: hay que citar la autoría y no se puede usar con fines comerciales.

**Certificado de discapacidad del 33%.** Es el grado mínimo que da acceso a la mayoría de medidas de empleo.

**Cuota de reserva.** Las empresas públicas y privadas con 50 o más trabajadores deben tener al menos un 2% de plantilla con discapacidad igual o superior al 33% (artículo 42 del Real Decreto Legislativo 1/2013). No hay datos oficiales públicos de qué empresas cumplen.

**Centros Especiales de Empleo (CEE).** Empresas con al menos un 70% de plantilla con discapacidad. Las personas con discapacidad intelectual con un 33% o más cuentan como colectivo con especiales dificultades, así que reciben más apoyo público al ser contratadas. Cada comunidad autónoma tiene un registro público de CEE.

**Empleo con apoyo.** Modelo en el que un preparador laboral acompaña a la persona en la búsqueda y en el puesto. Una herramienta clave es el análisis de tareas: dividir un trabajo en pasos simples.

**ATS (Applicant Tracking System).** Software que usan las empresas para filtrar CV de forma automática. Prefiere formatos simples, sin tablas ni columnas, y con palabras clave de la oferta.

Fuentes: Grupo2000 (cuota de reserva) · Región de Murcia (calificación de CEE) · BOJA 2023 (especiales dificultades) · ARASAAC

## Módulos de la app

Todo se apoya en dos piezas centrales: el **perfil de la persona** y el **motor de lectura fácil**. Los módulos de encima solo leen el perfil y pasan sus textos por el motor.

### Pieza central A: motor de lectura fácil

- Entrada: un texto cualquiera (oferta, carta, CV). Salida: el texto adaptado a lectura fácil.
- Añade pictogramas de ARASAAC a las palabras clave.
- Calcula un índice de legibilidad en español (Fernández-Huerta o INFLESZ) antes y después.
- Comprueba reglas automáticas: longitud de frase, siglas, números, palabras difíciles.
- Opción de leer el texto en voz alta.
- Se empaqueta también como **servidor MCP / skill** reutilizable por otras apps.

### Pieza central B: perfil de la persona

- Datos: intereses, habilidades, experiencia, formación, apoyos que necesita, zona y transporte.
- Campo opcional: si tiene certificado de discapacidad y si quiere que se use en la búsqueda.

### Módulo 1: entrevista guiada

- El agente hace preguntas sencillas en lectura fácil, con pictogramas y opción de respuesta por voz.
- Convierte experiencias cotidianas en competencias profesionales. Ejemplo: "ponía las mesas en el comedor" pasa a "montaje de sala y atención en comedor".
- Rellena el perfil. El preparador laboral lo revisa.

### Módulo 2: CV doble

- **Versión ATS**: formato limpio de una columna, palabras clave de la oferta y puntuación de encaje con explicación.
- **Versión en lectura fácil**: el mismo CV explicado a la persona, para que entienda lo que pone.
- Exportación a PDF y DOCX.

### Módulo 3: ofertas con filtro del 33%

- **Todas las ofertas se muestran en lectura fácil por defecto**, con pictogramas, lectura en voz alta y un botón para ver el texto original.
- La versión adaptada de cada oferta se guarda para no repetir llamadas al LLM.
- Busca ofertas en fuentes públicas y portales específicos de discapacidad.
- Un LLM clasifica cada oferta: pide certificado, es un CEE, es empresa de 50 o más trabajadores, o es oferta general.
- Cruza las empresas con los registros públicos de CEE.
- Da una puntuación de encaje con el perfil, explicada en lectura fácil.
- Genera el análisis de tareas del puesto con pictogramas.
- El filtro del 33% solo se activa si la persona lo ha elegido en su perfil.

### Módulo 4: cartas de presentación

- Carta personalizada para cada oferta, a partir del perfil.
- Versión en lectura fácil para que la persona sepa qué se envía en su nombre.
- Si la persona lo elige, menciona el certificado de forma positiva.

### Módulo 5: preparar la entrevista

- Preguntas probables según la oferta, explicadas en lectura fácil.
- Práctica por voz con sugerencias amables.

## Arquitectura y stack

Una web en React habla con una API en Java con Spring Boot. La API contiene los agentes de cada módulo, que leen el perfil y pasan todo por el motor de lectura fácil.

```
Persona que busca empleo        Preparador laboral
            \                      /
     Web accesible · React + TypeScript
     (lectura fácil, pictogramas, voz y botones grandes)
                     |
     API · Spring Boot + Spring AI (agentes y módulos)
     [Entrevista] [CV doble] [Ofertas] [Cartas] [Simulador]
            |                          |
   Perfil de la persona       Motor de lectura fácil
   (PostgreSQL, datos mínimos) (servidor MCP / skill)
                     |                 |
   Servicios externos: fuentes de ofertas y registros de CEE · API de ARASAAC
   API del LLM (Claude u otro proveedor): la usan los agentes y el motor
```

El motor se construye primero y como pieza independiente (servidor MCP), así sirve para esta app y para cualquier otra.

| Capa | Tecnología | Notas |
|---|---|---|
| Backend | Java 21, Spring Boot 4 | Es lo que más piden las ofertas de empleo |
| IA | Spring AI 2 con Claude (Anthropic) | ChatClient, salidas estructuradas a records de Java y tool calling. Permite cambiar de proveedor |
| Agentes | Bucle de agente propio con el tool calling de Spring AI | Primero a mano para entenderlo |
| MCP | Starters de MCP de Spring AI | Para publicar el motor de lectura fácil como servidor MCP |
| Base de datos | PostgreSQL con Spring Data JPA, Flyway para migraciones | H2 o Testcontainers en los tests |
| Frontend | React, TypeScript, Vite | Accesibilidad WCAG 2.2 AA como requisito |
| Voz | Web Speech API del navegador | Suficiente para empezar |
| Documentos | OpenPDF (PDF), Apache POI (DOCX) | Para el CV y las cartas |
| Tests | JUnit 5, Mockito, Testcontainers, Vitest | Un test por tarea como mínimo |
| Build y despliegue | Maven, Docker, Render o Fly.io | Fase 10 |

Python queda como opcional para scripts de evaluación. Los índices de legibilidad se programan en Java: contar sílabas en español es un buen ejercicio de la fase 2.

> Decisión de la fase 0: para empezar usamos un modelo local con **Ollama** (gratis). Spring AI permite pasar a Claude más adelante cambiando el starter y la configuración.

**Estructura de carpetas inicial:**

```
empleo-facil/
├── README.md
├── APRENDIZAJE.md
├── EVALUACION.md
├── docs/proyecto.md          # este documento
├── backend/                  # Spring Boot + Maven
│   ├── pom.xml
│   └── src/
│       ├── main/java/es/empleofacil/
│       │   ├── llm/           # configuración de Spring AI y prompts
│       │   ├── lecturafacil/  # motor: adaptación, reglas, legibilidad
│       │   ├── pictogramas/   # cliente de ARASAAC
│       │   ├── perfil/
│       │   ├── cv/
│       │   ├── ofertas/
│       │   └── cartas/
│       └── test/java/es/empleofacil/
├── mcp-lectura-facil/        # servidor MCP con Spring AI (fase 5)
├── frontend/                 # React + TypeScript (fase 3)
└── evals/                    # textos y ofertas de prueba
```

El nombre `empleo-facil` es provisional.

## Fases y hitos

El proyecto va en 11 fases pequeñas. Al terminar la fase 4 ya hay un proyecto completo para enseñar en el CV; el resto suma.

| Fase | Objetivo | Qué aprendo | Entregable |
|---|---|---|---|
| 0. Preparación | Repositorio, proyecto Spring Boot y primera llamada a un LLM | Spring Initializr, Maven, ChatClient de Spring AI, claves en variables de entorno, conceptos de prompt, tokens y temperatura, CI con GitHub Actions | Endpoint que envía un texto al modelo y devuelve la respuesta |
| 1. Motor de lectura fácil v1 | Adaptar un texto a lectura fácil | Ingeniería de prompts, prompt de sistema, ejemplos (few-shot), salidas estructuradas a records de Java | Endpoint REST: entra un texto, sale adaptado y con lista de cambios |
| 2. Reglas y legibilidad | Comprobar el resultado con código, no solo con IA | Validación determinista, índices Fernández-Huerta e INFLESZ programados en Java, tests con JUnit | Informe antes/después con puntuación y reglas que fallan |
| 3. Pictogramas y primera web | API de ARASAAC y una interfaz sencilla | Consumir APIs con RestClient, CORS, React con TypeScript, accesibilidad web | Web: pegas un texto y ves la versión con pictogramas y lectura en voz alta |
| 4. Evaluación | Medir si el motor funciona bien | Conjunto de pruebas, métricas, LLM como juez, comparar prompts y modelos | `EVALUACION.md` con resultados sobre 30 a 50 textos reales |
| 5. Servidor MCP / skill | Convertir el motor en una pieza reutilizable | Protocolo MCP, starters de MCP de Spring AI, documentación | Servidor MCP de lectura fácil publicado en GitHub |
| 6. Perfil y entrevista guiada | Conversación que rellena el perfil | Agentes conversacionales, memoria de conversación de Spring AI, extracción de datos, Spring Data JPA y PostgreSQL | Entrevista en lectura fácil que guarda el perfil |
| 7. CV doble | CV en versión ATS y en lectura fácil | Plantillas, palabras clave, exportar a PDF con OpenPDF y a DOCX con Apache POI | Descarga de los dos CV a partir del perfil |
| 8. Ofertas y filtro del 33% | Buscar, clasificar y mostrar ofertas en lectura fácil | Bucle de agente con tool calling, clasificación con LLM, caché de resultados, cruce con registros de CEE, uso responsable de fuentes | Lista de ofertas filtradas, en lectura fácil, con encaje explicado y análisis de tareas |
| 9. Cartas y entrevista | Carta por oferta y práctica de entrevista | Personalización con contexto, voz a texto y texto a voz | Carta descargable y simulador de entrevista |
| 10. Despliegue y validación | Publicar la app y probarla con personas reales | Docker, despliegue en la nube (CD), recoger feedback | Demo online, vídeo de 2 minutos y README final |

**Regla de avance:** una fase se cierra cuando su entregable funciona, tiene tests y está anotada en `APRENDIZAJE.md`.

## Principios éticos y privacidad

La app trabaja con datos de salud y con personas en situación de vulnerabilidad. Estos principios son requisitos, no extras.

1. **La discapacidad la cuenta la persona.** El certificado nunca aparece en el CV ni en las cartas por defecto. La persona decide si se usa, y lo puede cambiar en cualquier momento.
2. **Humano en el bucle.** Nada se envía a una empresa sin que la persona y, si existe, su preparador laboral lo hayan revisado.
3. **La persona entiende todo.** Cada documento que sale en su nombre tiene su versión en lectura fácil.
4. **Datos mínimos.** Solo se guarda lo necesario. El grado de discapacidad es un dato de salud especialmente protegido por el RGPD.
5. **Nada de datos reales en desarrollo.** Para programar y evaluar se usan perfiles inventados.
6. **Sin inventar.** El agente no añade experiencia ni títulos que la persona no tenga.
7. **Transparencia.** La app explica por qué recomienda una oferta y dice cuando un texto lo ha generado una IA.
8. **Fuentes respetadas.** Se cumplen los términos de uso de los portales de empleo y la licencia de ARASAAC (citar autoría, uso no comercial).

## Evaluación y presentación en el CV

Lo que diferencia este proyecto es poder demostrar con datos que funciona.

**Cómo se evalúa:**

- **Motor de lectura fácil:** índice de legibilidad antes y después, porcentaje de reglas cumplidas y revisión de un LLM juez con una rúbrica. Se compara si el significado se mantiene.
- **Clasificador de ofertas:** 50 a 100 ofertas etiquetadas a mano. Se mide la precisión al detectar ofertas para personas con certificado y CEE.
- **CV ATS:** comprobar que un parser de CV extrae bien los campos y medir la coincidencia de palabras clave con la oferta.
- **Validación humana:** si es posible, probar textos con personas lectoras y preparadores laborales de alguna entidad del sector.

**Qué debe tener el repositorio:**

- README con el problema, una captura o GIF, cómo ejecutarlo y la arquitectura.
- `EVALUACION.md` con resultados y conclusiones.
- `APRENDIZAJE.md` con el diario de cada fase.
- Demo online y vídeo de 2 minutos.

**Frase para el CV (borrador):**

> Diseñé y desarrollé una app con IA generativa que ayuda a personas con discapacidad intelectual a buscar empleo: agentes con herramientas, servidor MCP de lectura fácil, clasificación de ofertas con LLM y CV optimizado para ATS. Evaluado con [X] textos y validado con usuarios reales.

**Palabras clave que cubre:** IA generativa, LLM, agentes, tool use, MCP, skills, ingeniería de prompts, salidas estructuradas, evaluación de LLM, IA responsable, accesibilidad, Java, Spring Boot, Spring AI, TypeScript, React.
