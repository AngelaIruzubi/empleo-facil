// Tipos de la respuesta de /api/easy-read y de /api/easy-read/stream. Son los mismos records que en el backend.

export type Pictogram = {
  id: number
  keyword: string
  imageUrl: string
}

export type Change = {
  original: string
  adapted: string
  reason: string
}

export type ReadabilityScore = {
  value: number
  level: string
}

export type Readability = {
  sentences: number
  words: number
  syllables: number
  fernandezHuerta: ReadabilityScore
  inflesz: ReadabilityScore
}

export type RuleViolation = {
  rule: string
  severity: 'ERROR' | 'WARNING'
  fragment: string
  sentence: string
  advice: string
}

export type EasyReadReport = {
  adaptedText: string
  changes: Change[]
  professions: string[]
  pictogram: Pictogram | null
  readabilityBefore: Readability
  readabilityAfter: Readability
  violations: RuleViolation[]
}

// Un error que se puede enseñar a la persona: el backend ya lo escribe en lectura fácil
export class ApiError extends Error {
  readonly title: string
  readonly detail: string

  constructor(title: string, detail: string) {
    super(detail)
    this.title = title
    this.detail = detail
  }
}

export const MAX_TEXT_LENGTH = 4000

const AI_UNAVAILABLE = new ApiError(
  'No se puede adaptar el texto ahora',
  'Ahora no podemos adaptar el texto.\nInténtalo otra vez dentro de un rato.',
)

export async function adaptText(text: string): Promise<EasyReadReport> {
  const response = await post('/api/easy-read', { text })
  return response.json()
}

// Lo que va llegando mientras la IA escribe, en este orden:
// onPictogram: el pictograma de la profesión, al principio (si la IA ha dado una profesión con dibujo).
// onText: todo el texto que ha llegado hasta ahora, para enseñarlo en pantalla.
// onTextEnd: el texto está completo. Después la IA aún escribe la lista de cambios.
export type StreamHandlers = {
  onPictogram: (pictogram: Pictogram) => void
  onText: (textSoFar: string) => void
  onTextEnd: () => void
}

// Igual que adaptText, pero el servidor manda el pictograma y el texto a trozos según la IA lo escribe
export async function adaptTextStreaming(
  text: string,
  { onPictogram, onText, onTextEnd }: StreamHandlers,
): Promise<EasyReadReport> {
  const response = await post('/api/easy-read/stream', { text })
  if (!response.body) {
    throw AI_UNAVAILABLE
  }
  const reader = response.body.pipeThrough(new TextDecoderStream()).getReader()
  let buffer = ''
  let textSoFar = ''
  for (;;) {
    let chunk: ReadableStreamReadResult<string>
    try {
      chunk = await reader.read()
    } catch {
      // Se ha cortado la conexión a mitad
      throw AI_UNAVAILABLE
    }
    if (chunk.done) {
      // El stream ha terminado sin informe: algo ha fallado en el servidor
      throw AI_UNAVAILABLE
    }
    const { events, rest } = takeEvents(buffer + chunk.value)
    buffer = rest
    for (const { event, data } of events) {
      if (event === 'pictogram') {
        onPictogram(JSON.parse(data))
      } else if (event === 'text') {
        textSoFar += JSON.parse(data).text
        onText(textSoFar)
      } else if (event === 'text-end') {
        onTextEnd()
      } else if (event === 'report') {
        await reader.cancel()
        return JSON.parse(data)
      } else if (event === 'error') {
        await reader.cancel()
        throw problemToError(JSON.parse(data))
      }
    }
  }
}

// Una frase que el preparador laboral ha marcado como mal, con su comentario
export type Correction = {
  sentence: string
  comment: string
}

// La IA corrige solo las frases marcadas. Devuelve una frase corregida por cada corrección, en el mismo orden.
// Una frase corregida puede tener varias líneas si la IA la ha dividido en frases más cortas.
export async function reviseSentences(
  originalText: string,
  adaptedText: string,
  corrections: Correction[],
): Promise<string[]> {
  const response = await post('/api/easy-read/revise', { originalText, adaptedText, corrections })
  const { sentences } = (await response.json()) as { sentences: string[] }
  return sentences
}

export type ServerSentEvent = { event: string; data: string }

// Formato Server-Sent Events: cada mensaje son líneas "event:..." y "data:..." y acaba con una línea en blanco.
// Devuelve los mensajes completos y el trozo final, que espera al siguiente paquete.
export function takeEvents(buffer: string): { events: ServerSentEvent[]; rest: string } {
  const blocks = buffer.replace(/\r\n?/g, '\n').split('\n\n')
  const rest = blocks.pop() ?? ''
  const events = blocks
    .map((block) => {
      let event = 'message'
      const data: string[] = []
      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) {
          event = line.slice('event:'.length).trim()
        } else if (line.startsWith('data:')) {
          data.push(line.slice('data:'.length).replace(/^ /, ''))
        }
      }
      return { event, data: data.join('\n') }
    })
    .filter(({ data }) => data !== '')
  return { events, rest }
}

async function post(url: string, body: unknown): Promise<Response> {
  let response: Response
  try {
    response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
  } catch {
    throw new ApiError(
      'No hay conexión con el servidor',
      'La web no puede conectar con el servidor.\nComprueba que el servidor está encendido.',
    )
  }
  if (!response.ok) {
    // El backend responde con ProblemDetail: un título y un detalle en lectura fácil
    throw problemToError(await response.json().catch(() => null))
  }
  return response
}

function problemToError(problem: { title?: string; detail?: string } | null): ApiError {
  if (!problem?.title || !problem.detail) {
    return AI_UNAVAILABLE
  }
  return new ApiError(problem.title, problem.detail)
}
