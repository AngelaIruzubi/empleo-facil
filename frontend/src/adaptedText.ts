// Convierte el texto adaptado en bloques para dibujarlos con su etiqueta HTML.
// La IA escribe cada frase en una línea, deja una línea en blanco entre párrafos
// y empieza los elementos de una lista con un guion.

export type Block =
  | { kind: 'heading'; text: string }
  | { kind: 'sentence'; text: string }
  | { kind: 'list'; items: string[] }

export type AdaptedText = {
  // El título es el primer párrafo si solo tiene una línea, por ejemplo "Buscamos persona para limpiar oficinas"
  title: string | null
  // Cada párrafo es una lista de bloques
  paragraphs: Block[][]
}

const LIST_ITEM = /^\s*-\s+/

export function parseAdaptedText(text: string): AdaptedText {
  const paragraphs = text
    .split(/\n\s*\n/)
    .map((paragraph) => paragraph.split('\n').map((line) => line.trim()).filter((line) => line !== ''))
    .filter((lines) => lines.length > 0)

  let title: string | null = null
  const first = paragraphs[0]
  if (first && first.length === 1 && !LIST_ITEM.test(first[0])) {
    title = first[0]
    paragraphs.shift()
  }

  return { title, paragraphs: paragraphs.map(toBlocks) }
}

// Todas las líneas en el orden en que se ven: título, preguntas, frases y elementos de lista.
// Es el orden de lectura en voz alta. Result numera las líneas en este mismo orden para resaltar la que suena.
export function readingOrder({ title, paragraphs }: AdaptedText): string[] {
  const lines = title ? [title] : []
  for (const blocks of paragraphs) {
    for (const block of blocks) {
      if (block.kind === 'list') {
        lines.push(...block.items)
      } else {
        lines.push(block.text)
      }
    }
  }
  return lines
}

function toBlocks(lines: string[]): Block[] {
  const blocks: Block[] = []
  lines.forEach((line, index) => {
    if (LIST_ITEM.test(line)) {
      const item = line.replace(LIST_ITEM, '')
      const last = blocks.at(-1)
      if (last?.kind === 'list') {
        last.items.push(item)
      } else {
        blocks.push({ kind: 'list', items: [item] })
      }
    } else if (index === 0 && line.endsWith('?') && lines.length > 1) {
      // Una pregunta al principio de un párrafo hace de título: "¿Qué tienes que hacer?"
      blocks.push({ kind: 'heading', text: line })
    } else {
      blocks.push({ kind: 'sentence', text: line })
    }
  })
  return blocks
}
