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

// Cambia unas líneas del texto por otras. Las líneas se cuentan en el orden de readingOrder:
// cada línea con letras del texto es una línea de lectura (título, pregunta, frase o elemento de lista).
// Una línea nueva puede traer varias frases separadas por saltos de línea.
// Si la línea vieja era un elemento de lista, las nuevas también lo son.
// Devuelve el texto nuevo, los números de las líneas cambiadas ya contadas en el texto nuevo
// y newLineOf: dónde queda cada línea vieja en el texto nuevo (si una línea de antes se ha
// dividido en dos, las de después bajan un puesto).
export function replaceLines(
  text: string,
  replacements: Map<number, string>,
): { text: string; changedLines: number[]; newLineOf: number[] } {
  const result: string[] = []
  const changedLines: number[] = []
  const newLineOf: number[] = []
  let oldLine = 0
  let newLine = 0
  for (const raw of text.split('\n')) {
    if (raw.trim() === '') {
      result.push(raw)
      continue
    }
    const replacement = replacements.get(oldLine)
    newLineOf[oldLine] = newLine
    oldLine++
    if (replacement === undefined) {
      result.push(raw)
      newLine++
      continue
    }
    const listPrefix = LIST_ITEM.test(raw) ? '- ' : ''
    const sentences = replacement
      .split('\n')
      .map((sentence) => sentence.replace(LIST_ITEM, '').trim())
      .filter((sentence) => sentence !== '')
    for (const sentence of sentences) {
      result.push(listPrefix + sentence)
      changedLines.push(newLine)
      newLine++
    }
  }
  return { text: result.join('\n'), changedLines, newLineOf }
}

// Dos frases son iguales si solo cambian las mayúsculas, los espacios o el punto final
export function sameSentence(a: string, b: string): boolean {
  const normalize = (sentence: string) =>
    sentence
      .toLowerCase()
      .replace(/\s+/g, ' ')
      .replace(/[.!?\s]+$/, '')
      .trim()
  return normalize(a) === normalize(b)
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
