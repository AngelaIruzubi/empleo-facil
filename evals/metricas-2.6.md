# Métricas de la fase 2

Resultados de `evals/resultados` medidos con el código de la fase 2 (`RuleChecker` y `ReadabilityCalculator`). Lo genera `evals/EvalMetrics.java`.

INFLESZ: cuanto más alto, más fácil. Menos de 40 es muy difícil; de 65 a 80, bastante fácil.

## gemma3-4b

| Texto | INFLESZ antes | INFLESZ después | Errores | Avisos | Reglas que fallan |
|---|---|---|---|---|---|
| 01-oferta-reposicion | 40.2 | 67.4 | 2 | 3 | FORBIDDEN_SIGN ×2, ACRONYM ×2, VAGUE_WORD ×1 |
| 02-oferta-limpieza | 42.7 | 74.8 | 1 | 1 | PERCENTAGE ×1, SLASH ×1 |
| 03-correo-rechazo | 38.4 | 65.4 | 0 | 0 | — |
| 04-primer-dia | 70.7 | 85.0 | 0 | 1 | ACRONYM ×1 |
| 05-contrato-prueba | 48.2 | 79.4 | 0 | 0 | — |

## llama3.2-3b

| Texto | INFLESZ antes | INFLESZ después | Errores | Avisos | Reglas que fallan |
|---|---|---|---|---|---|
| 01-oferta-reposicion | 40.2 | 69.2 | 2 | 2 | LONG_SENTENCE ×1, FORBIDDEN_SIGN ×1, MENTE_WORD ×1, ACRONYM ×1 |
| 02-oferta-limpieza | 42.7 | 71.9 | 1 | 1 | PERCENTAGE ×1, ACRONYM ×1 |
| 03-correo-rechazo | 38.4 | 77.2 | 1 | 1 | LONG_SENTENCE ×1, SLASH ×1 |
| 04-primer-dia | 70.7 | 77.8 | 1 | 2 | FORBIDDEN_SIGN ×1, ACRONYM ×2 |
| 05-contrato-prueba | 48.2 | 74.5 | 0 | 0 | — |

## Resumen

| Modelo | INFLESZ medio antes | INFLESZ medio después | Errores | Avisos |
|---|---|---|---|---|
| gemma3-4b | 48.0 | 74.4 | 3 | 5 |
| llama3.2-3b | 48.0 | 74.1 | 5 | 6 |

## Conclusión

_Pendiente: la escribe Angela._
