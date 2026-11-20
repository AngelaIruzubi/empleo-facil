import { describe, expect, it } from 'vitest'
import { parseAdaptedText, readingOrder, replaceLines, sameSentence } from './adaptedText'

// Un texto adaptado como los que escribe la IA: título, una frase, una pregunta con lista y otra con frases
const TEXT = [
  'Buscamos camarero',
  '',
  'Un restaurante busca un camarero.',
  '',
  '¿Qué tienes que hacer?',
  '- Servir mesas.',
  '- Limpiar.',
  '',
  '¿Cuánto ganas?',
  'Las pagas son dinero extra.',
].join('\n')

describe('parseAdaptedText', () => {
  it('separa el título, las preguntas, las frases y las listas', () => {
    expect(parseAdaptedText(TEXT)).toEqual({
      title: 'Buscamos camarero',
      paragraphs: [
        [{ kind: 'sentence', text: 'Un restaurante busca un camarero.' }],
        [
          { kind: 'heading', text: '¿Qué tienes que hacer?' },
          { kind: 'list', items: ['Servir mesas.', 'Limpiar.'] },
        ],
        [
          { kind: 'heading', text: '¿Cuánto ganas?' },
          { kind: 'sentence', text: 'Las pagas son dinero extra.' },
        ],
      ],
    })
  })

  it('no pone título si el primer párrafo tiene varias líneas', () => {
    expect(parseAdaptedText('Hola.\nAdiós.').title).toBeNull()
  })

  it('una pregunta sola no es título de párrafo', () => {
    expect(parseAdaptedText('Hola\n\n¿Vienes?').paragraphs).toEqual([[{ kind: 'sentence', text: '¿Vienes?' }]])
  })
})

describe('readingOrder', () => {
  it('da las líneas en el orden en que se leen, sin guiones', () => {
    expect(readingOrder(parseAdaptedText(TEXT))).toEqual([
      'Buscamos camarero',
      'Un restaurante busca un camarero.',
      '¿Qué tienes que hacer?',
      'Servir mesas.',
      'Limpiar.',
      '¿Cuánto ganas?',
      'Las pagas son dinero extra.',
    ])
  })
})

describe('replaceLines', () => {
  // Las líneas se cuentan como en readingOrder: 0 es el título, 3 es «Servir mesas.», 6 es la de las pagas
  it('cambia una frase y deja el resto igual', () => {
    const { text, changedLines } = replaceLines(TEXT, new Map([[6, 'El sueldo ya incluye la paga extra.']]))

    expect(text).toBe(TEXT.replace('Las pagas son dinero extra.', 'El sueldo ya incluye la paga extra.'))
    expect(changedLines).toEqual([6])
  })

  it('un elemento de lista sigue siendo de la lista', () => {
    const { text } = replaceLines(TEXT, new Map([[3, 'Llevar los platos a las mesas.']]))

    expect(text).toContain('- Llevar los platos a las mesas.\n- Limpiar.')
  })

  it('una frase partida en dos va en su sitio y las líneas de después bajan un puesto', () => {
    const { text, changedLines, newLineOf } = replaceLines(TEXT, new Map([[3, 'Servir mesas.\nRecoger los platos.']]))

    expect(readingOrder(parseAdaptedText(text)).slice(3, 6)).toEqual(['Servir mesas.', 'Recoger los platos.', 'Limpiar.'])
    expect(changedLines).toEqual([3, 4])
    expect(newLineOf[4]).toBe(5)
    expect(newLineOf[6]).toBe(7)
  })

  it('una línea vacía quita la frase', () => {
    const { text, changedLines, newLineOf } = replaceLines(TEXT, new Map([[4, '']]))

    expect(text).not.toContain('Limpiar.')
    expect(changedLines).toEqual([])
    expect(newLineOf[5]).toBe(4)
  })

  it('añadir es dejar la frase y poner la nueva debajo', () => {
    const { text, changedLines } = replaceLines(TEXT, new Map([[4, 'Limpiar.\nDe 9 a 13 horas.']]))

    expect(text).toContain('- Limpiar.\n- De 9 a 13 horas.')
    expect(changedLines).toEqual([4, 5])
  })

  it('quita el guion si la IA lo escribe ella misma', () => {
    const { text } = replaceLines(TEXT, new Map([[3, '- Servir comidas.']]))

    expect(text).toContain('- Servir comidas.')
    expect(text).not.toContain('- - ')
  })
})

describe('sameSentence', () => {
  it('no tiene en cuenta mayúsculas, espacios ni el punto final', () => {
    expect(sameSentence('Trabajas en una residencia.', ' trabajas  en una residencia ')).toBe(true)
  })

  it('una palabra distinta ya es otra frase', () => {
    expect(sameSentence('Dar el desayuno.', 'Preparar el desayuno.')).toBe(false)
  })
})
