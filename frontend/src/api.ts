// Tipos de la respuesta de /api/easy-read. Son los mismos records que en el backend.

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

export async function adaptText(text: string): Promise<EasyReadReport> {
  let response: Response
  try {
    response = await fetch('/api/easy-read', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text }),
    })
  } catch {
    throw new ApiError(
      'No hay conexión con el servidor',
      'La web no puede conectar con el servidor.\nComprueba que el servidor está encendido.',
    )
  }

  if (!response.ok) {
    // El backend responde con ProblemDetail: un título y un detalle en lectura fácil
    const problem = await response.json().catch(() => null)
    throw new ApiError(
      problem?.title ?? 'No se puede adaptar el texto ahora',
      problem?.detail ?? 'Ahora no podemos adaptar el texto.\nInténtalo otra vez dentro de un rato.',
    )
  }
  return response.json()
}
