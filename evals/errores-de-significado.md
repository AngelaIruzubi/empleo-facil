# Errores de significado

Casos reales en los que la IA cambió el significado del texto original.
Las reglas de la fase 2 no los detectan: las frases son cortas y cumplen las pautas.
Sirven para la fase 4: comprobar si un cambio de prompt o de modelo los arregla
y si una IA juez es capaz de detectarlos.

Solo se guarda la frase necesaria, nunca la oferta entera ni datos personales.

| # | Texto original | Lo que escribió la IA | Versión correcta | Tipo de error |
|---|---|---|---|---|
| 1 | Es posible que se cambie el horario para cuadrar con los turnos de otros compañeros. | Es posible que tengas que cambiar de horario entre semana para que todos trabajen juntos. | Puede que te cambien el horario.<br>Así cubres los turnos de otros compañeros. | Cambia el motivo |
| 2 | Requisitos: ESO | Tienes que haber terminado el Bachillerato (ESO). | _Pendiente_ | Confunde un título por otro |
| 3 | Se valorará CAP | Es bueno tener el Certificado de Acceso Profesional (CAP). | _Pendiente_ | Se inventa el significado de una sigla |
| 4 | Oferta sin dirección del lugar de trabajo | ¿Dónde es el trabajo?<br>Las oficinas están en un lugar. | No se dice nada del lugar | Añade información que no está |

Fuentes: caso 1, prueba de Angela en la web (fase 3). Casos 2 y 3, `evals/resultados/gemma3-4b/01-oferta-reposicion.txt` (fase 1).
Caso 4, prueba de la API en la tarea 2.5.
