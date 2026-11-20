import { afterEach, describe, expect, it, vi } from 'vitest'
import { adaptTextStreaming, ApiError, reviseSentences, takeEvents, type EasyReadReport } from './api'

const PICTOGRAM = { id: 1, keyword: 'camarero', imageUrl: 'https://static.arasaac.org/pictograms/1/1_300.png' }
const REPORT = { adaptedText: 'Buscamos camarero.\nEs fácil.', pictogram: PICTOGRAM } as unknown as EasyReadReport

// Un mensaje SSE como los que manda el servidor
const event = (name: string, data: unknown) => `event:${name}\ndata:${JSON.stringify(data)}\n\n`

// Simula fetch: el servidor contesta con estos trozos, uno detrás de otro
function serverSends(chunks: string[], status = 200) {
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      for (const chunk of chunks) {
        controller.enqueue(new TextEncoder().encode(chunk))
      }
      controller.close()
    },
  })
  const fetchMock = vi.fn(async () => new Response(body, { status }))
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

const noHandlers = { onPictogram: () => {}, onText: () => {}, onTextEnd: () => {} }

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('takeEvents', () => {
  it('saca los mensajes completos y guarda el trozo que falta', () => {
    expect(takeEvents('event:text\ndata:{"text":"Hola"}\n\nevent:te')).toEqual({
      events: [{ event: 'text', data: '{"text":"Hola"}' }],
      rest: 'event:te',
    })
  })

  it('acepta saltos de línea de Windows y junta varias líneas data', () => {
    expect(takeEvents('event:x\r\ndata:a\r\ndata:b\r\n\r\n').events).toEqual([{ event: 'x', data: 'a\nb' }])
  })

  it('ignora los comentarios que el servidor manda para saber si la página sigue abierta', () => {
    expect(takeEvents(':sigo\n\n').events).toEqual([])
  })
})

describe('adaptTextStreaming', () => {
  it('avisa del pictograma, de cada trozo de texto y del final, y devuelve el informe', async () => {
    // Los trozos cortan los mensajes por la mitad, como pasa de verdad en la red
    const stream = event('pictogram', PICTOGRAM) + event('text', { text: 'Buscamos camarero.' }) +
      ':sigo\n\n' + event('text', { text: '\nEs fácil.' }) + event('text-end', { complete: true }) + event('report', REPORT)
    serverSends([stream.slice(0, 30), stream.slice(30, 95), stream.slice(95)])
    const seen: string[] = []

    const report = await adaptTextStreaming('Se precisa camarero', {
      onPictogram: (pictogram) => seen.push(`pictograma ${pictogram.keyword}`),
      onText: (textSoFar) => seen.push(textSoFar),
      onTextEnd: () => seen.push('texto completo'),
    })

    expect(seen).toEqual(['pictograma camarero', 'Buscamos camarero.', 'Buscamos camarero.\nEs fácil.', 'texto completo'])
    expect(report).toEqual(REPORT)
  })

  it('un evento de error se convierte en un ApiError con el mensaje del servidor', async () => {
    serverSends([event('error', { title: 'No se puede adaptar el texto ahora', detail: 'Inténtalo luego.' })])

    await expect(adaptTextStreaming('Texto', noHandlers)).rejects.toEqual(
      new ApiError('No se puede adaptar el texto ahora', 'Inténtalo luego.'),
    )
  })

  it('si el stream se acaba sin informe, avisa de que la IA no está disponible', async () => {
    serverSends([event('text', { text: 'Busca' })])

    await expect(adaptTextStreaming('Texto', noHandlers)).rejects.toMatchObject({
      title: 'No se puede adaptar el texto ahora',
    })
  })

  it('un 400 con ProblemDetail se enseña tal cual', async () => {
    serverSends([JSON.stringify({ title: 'Revisa el texto', detail: 'Escribe un texto para adaptar.' })], 400)

    await expect(adaptTextStreaming('   ', noHandlers)).rejects.toEqual(
      new ApiError('Revisa el texto', 'Escribe un texto para adaptar.'),
    )
  })

  it('si no hay conexión con el servidor, lo dice', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Promise.reject(new TypeError('Failed to fetch'))))

    await expect(adaptTextStreaming('Texto', noHandlers)).rejects.toMatchObject({
      title: 'No hay conexión con el servidor',
    })
  })
})

describe('reviseSentences', () => {
  it('manda la oferta, el texto y las frases marcadas, y devuelve las frases corregidas', async () => {
    const fetchMock = serverSends([JSON.stringify({ sentences: ['El sueldo ya incluye la paga extra.'] })])
    const corrections = [{ sentence: 'Las pagas son dinero extra.', comment: 'La paga ya está en el sueldo.' }]

    const sentences = await reviseSentences('Oferta', 'Texto adaptado', corrections)

    expect(sentences).toEqual(['El sueldo ya incluye la paga extra.'])
    expect(fetchMock).toHaveBeenCalledWith('/api/easy-read/revise', expect.objectContaining({ method: 'POST' }))
    const [, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit]
    expect(JSON.parse(init.body as string)).toEqual({ originalText: 'Oferta', adaptedText: 'Texto adaptado', corrections })
  })
})
