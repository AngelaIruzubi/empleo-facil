# Hechos clave

Para cada texto de `evals/textos/` hay un JSON con el mismo nombre. Dice lo que el texto adaptado
**no puede perder ni cambiar**. Es la «respuesta correcta» con la que se comprueba si la IA mantiene el significado.

```json
{
  "oferta": true,
  "profesiones_validas": ["camarero", "camarera"],
  "hechos": [
    { "tipo": "sueldo", "dato": "19.600 euros brutos al año, con las pagas extra repartidas en los meses" }
  ],
  "trampas": ["bruto: antes de quitar impuestos"]
}
```

- **oferta**: si el texto es una oferta de trabajo. Si no lo es, no debe salir pictograma de profesión.
- **profesiones_validas**: nombres de profesión que se dan por buenos para el pictograma.
- **hechos**: lo que tiene que estar en el texto adaptado, dicho con otras palabras si hace falta.
  Tipos: `puesto`, `funciones`, `requisito_obligatorio`, `requisito_valorable`, `contrato`, `jornada`,
  `horario`, `sueldo`, `fecha`, `como_apuntarse`, `mensaje` y `otros`.
- **trampas**: lo que suele hacer fallar a la IA en ese texto. No se puntúa: ayuda a leer los resultados.

Los textos 01 a 05 son de la fase 1. Los textos 06 a 36 se escribieron en la fase 4 (20 de noviembre de 2026),
inventados pero basados en cómo se escriben las ofertas reales, y sin datos personales.
Las fechas con día de la semana están comprobadas con código.

Las ofertas reales van en `evals/textos-privados/` y `evals/hechos-privados/`, que no se suben a GitHub.
