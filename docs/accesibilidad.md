# Accesibilidad de la web

Revisión de la tarea 3.7, hecha el 20 de noviembre de 2026.
Objetivo: cumplir WCAG 2.2 nivel AA y las pautas de diseño de [lectura-facil.md](lectura-facil.md).

## Herramientas

| Herramienta | Versión | Qué mira |
|---|---|---|
| Lighthouse | 12.8.2 | Nota de accesibilidad, rendimiento, buenas prácticas y SEO de la página al cargar |
| axe-core | 4.10.3 | Reglas de WCAG 2.0, 2.1 y 2.2 (A y AA) y buenas prácticas, en cada estado de la pantalla |
| Playwright + Chrome | 1.65 | Pruebas a mano automatizadas: teclado, foco, mensajes para el lector de pantalla, tamaños de pantalla |

Lighthouse y axe se ejecutan fuera del proyecto, con `npx` y en una carpeta temporal: no son dependencias de la web.

## Resultados

### Lighthouse (versión de producción, `npm run build` + `vite preview`)

| | Rendimiento | Accesibilidad | Buenas prácticas | SEO |
|---|---|---|---|---|
| Móvil | 98 | 100 | 100 | 100 |
| Escritorio | 100 | 100 | 100 | 100 |

### axe-core, en cada estado de la pantalla

| Estado | Problemas |
|---|---|
| 1. Página al abrir | 0 |
| 2. Esperando a la IA | 0 |
| 3. Error (la IA no responde) | 0 |
| 4. Texto adaptado con pictograma | 0 |
| 5. Revisión abierta con frases marcadas | 0 |

axe deja algunos contrastes «para revisar a mano» porque no sabe qué fondo hay detrás
(cuadros de texto y la oferta, que se queda fija al bajar). Están calculados a mano abajo.

### Contraste de colores

Mínimo de WCAG: 4,5:1 para texto y 3:1 para bordes y elementos de la interfaz.

| Combinación | Contraste |
|---|---|
| Texto sobre blanco | 14,64:1 |
| Texto suave (pistas, pie de página) sobre blanco | 7,86:1 |
| Texto sobre el cartel amarillo | 9,05:1 |
| Botón desactivado | 6,32:1 |
| Error en rojo sobre blanco | 6,54:1 |
| Texto de una frase marcada (fondo rosa) | 13,17:1 |
| Texto de una frase nueva (fondo amarillo claro) | 13,61:1 |
| Borde de una frase marcada | 5,88:1 |
| Borde de una frase nueva | 4,05:1 (antes 1,96:1, arreglado) |
| Contorno del foco sobre amarillo | 9,05:1 |

### Pruebas a mano

| Prueba | Resultado |
|---|---|
| Usar toda la web solo con el teclado | Bien. Orden del foco: texto, «Adaptar el texto», «Escuchar», «Revisar el texto». Las 4 opciones de cada frase se recorren con las flechas. |
| El foco no se pierde | Arreglado (ver abajo) |
| Mensajes para el lector de pantalla | Bien: «Estamos adaptando tu texto», «Ya sale el texto», «Ya está tu texto en lectura fácil», «Has marcado 1 frase», «Hecho: has cambiado 1 frase» |
| Pantalla de 320 px (como ampliar al 400 %, WCAG 1.4.10) | Sin scroll de lado |
| Más espacio en el texto (WCAG 1.4.12) | Nada se corta ni se tapa |
| Modo oscuro del sistema | La página se queda clara a propósito: lectura fácil pide mucho contraste y fondos lisos |
| Información solo por color | No hay: las frases marcadas y nuevas llevan la palabra «Quitar la marca» o «Nueva», los errores llevan título |
| Animaciones | No hay ninguna: lectura fácil pide evitar lo que distrae |

## Lo que se ha arreglado

1. **El foco se perdía al pulsar «Adaptar el texto»** (WCAG 2.4.3). El botón se desactivaba con `disabled`
   mientras la IA trabajaba, y un botón desactivado no puede tener el foco: el foco volvía al principio de la página.
   Ahora usa `aria-disabled`: se ve igual, el lector de pantalla dice que no está disponible y el foco se queda en él.
2. **El borde de las frases nuevas tenía poco contraste** (1,96:1). Ahora es un amarillo más oscuro, `#9a7300` (4,05:1).
3. **Mensaje sobrante para el lector de pantalla.** Después de hacer los cambios decía «No has marcado ninguna frase»
   justo antes de «Hecho…». Ahora solo dice «Hecho…».
4. **SEO.** Faltaban la descripción de la página y `robots.txt`.

## Lo que falta

- **Probar con un lector de pantalla de verdad** (NVDA en Windows o TalkBack en Android). Las pruebas de arriba
  comprueban qué textos se anuncian, pero no cómo suenan ni si el orden se entiende al escucharlo.
- **Probar con personas lectoras y preparadores laborales** (fase 10). Es la única forma de saber si el texto
  y la pantalla se entienden de verdad.
- **La voz de «Escuchar» depende del sistema.** La Web Speech API usa las voces instaladas en el ordenador o el móvil.
  Si el navegador no tiene voces, el botón no sale. Si no hay ninguna voz en español, lee con la voz que haya,
  y puede sonar con acento de otro idioma.
