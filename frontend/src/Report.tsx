import { useId, useState } from 'react'
import { parseAdaptedText, readingOrder, replaceLines } from './adaptedText'
import { ApiError, reviseSentences } from './api'

type ReportProps = {
  // El texto adaptado completo
  adaptedText: string
  // La oferta tal como la pegó la persona
  originalText: string
  // Se puede pedir la corrección cuando la IA ha terminado del todo (si no, el informe que llega pisaría el texto)
  canRevise: boolean
  // Avisa del texto nuevo con las frases corregidas, para enseñarlo arriba y leerlo con «Escuchar»
  onRevised: (adaptedText: string) => void
}

type Revision = { status: 'idle' } | { status: 'revising' } | { status: 'error'; title: string; detail: string }

// La revisión del preparador laboral: compara la oferta original con el texto adaptado,
// marca las frases que están mal con un comentario y pide a la IA que corrija solo esas frases.
// Va cerrado: la persona lectora no necesita esto. El preparador lo abre cuando quiere revisar.
// La legibilidad y las reglas no salen aquí: Angela decidió que no hacen falta para revisar.
// El servidor las sigue calculando para las evaluaciones de evals/.
function Report({ adaptedText, originalText, canRevise, onRevised }: ReportProps) {
  // Las frases marcadas: el número de línea y lo que ha escrito el preparador
  const [comments, setComments] = useState<Record<number, string>>({})
  // Las líneas que ha cambiado la última corrección, para que el preparador las revise otra vez
  const [corrected, setCorrected] = useState<number[]>([])
  const [revision, setRevision] = useState<Revision>({ status: 'idle' })

  const lines = readingOrder(parseAdaptedText(adaptedText))
  const markedLines = Object.keys(comments)
    .map(Number)
    .sort((a, b) => a - b)
  const missingComment = markedLines.some((line) => comments[line].trim() === '')
  const revising = revision.status === 'revising'

  function toggle(line: number) {
    setComments(({ [line]: current, ...others }) => (current === undefined ? { ...others, [line]: '' } : others))
  }

  async function revise() {
    setRevision({ status: 'revising' })
    try {
      const sentences = await reviseSentences(
        originalText,
        adaptedText,
        markedLines.map((line) => ({ sentence: lines[line], comment: comments[line] })),
      )
      const replacements = new Map(markedLines.map((line, index) => [line, sentences[index]]))
      const revised = replaceLines(adaptedText, replacements)
      setComments({})
      setCorrected(revised.changedLines)
      setRevision({ status: 'idle' })
      onRevised(revised.text)
    } catch (error) {
      const { title, detail } =
        error instanceof ApiError
          ? error
          : { title: 'No se pueden corregir las frases ahora', detail: 'Inténtalo otra vez dentro de un rato.' }
      setRevision({ status: 'error', title, detail })
    }
  }

  return (
    <details className="report">
      <summary>Revisar el texto (para el preparador laboral)</summary>
      <div className="report-body">
        <div>
          <p>Compara la oferta original con el texto adaptado. Si una frase está mal, pulsa «Está mal» y escribe qué falla.</p>
          <p>Mira sobre todo el sueldo, el horario, el tipo de contrato y los requisitos.</p>
        </div>

        <div className="review">
          <section className="review-original" aria-labelledby="review-original">
            <h3 id="review-original">Oferta original</h3>
            <p className="review-original-text">{originalText}</p>
          </section>

          <section aria-labelledby="review-adapted">
            <h3 id="review-adapted">Texto adaptado</h3>
            <ol className="review-lines">
              {lines.map((text, line) => (
                <ReviewLine
                  key={line}
                  text={text}
                  comment={comments[line]}
                  corrected={corrected.includes(line)}
                  disabled={revising}
                  onToggle={() => toggle(line)}
                  onComment={(comment) => setComments((current) => ({ ...current, [line]: comment }))}
                />
              ))}
            </ol>

            <div className="review-actions">
              <p className="review-count" role="status">
                {revising
                  ? 'La inteligencia artificial está corrigiendo las frases. Puede tardar 1 o 2 minutos.'
                  : markedLines.length === 0
                    ? 'No has marcado ninguna frase.'
                    : `Has marcado ${countLabel(markedLines.length, 'frase', 'frases')}.`}
              </p>
              {markedLines.length > 0 && !revising && (
                <>
                  <button type="button" onClick={revise} disabled={!canRevise || missingComment}>
                    Pedir a la IA que corrija {markedLines.length === 1 ? 'la frase marcada' : 'las frases marcadas'}
                  </button>
                  {!canRevise && <p className="hint">Puedes pedirlo cuando la inteligencia artificial termine.</p>}
                  {canRevise && missingComment && <p className="hint">Escribe qué está mal en cada frase marcada.</p>}
                </>
              )}
              {revision.status === 'error' && (
                <div className="error" role="alert">
                  {/* Sin h2: dentro de la sección «Texto adaptado» (h3) rompería el orden de los títulos */}
                  <p>
                    <strong>{revision.title}</strong>
                  </p>
                  <p>{revision.detail}</p>
                </div>
              )}
            </div>
          </section>
        </div>
      </div>
    </details>
  )
}

type ReviewLineProps = {
  text: string
  // undefined: la frase no está marcada
  comment: string | undefined
  // La ha cambiado la última corrección de la IA
  corrected: boolean
  // Mientras la IA corrige no se pueden cambiar las marcas
  disabled: boolean
  onToggle: () => void
  onComment: (comment: string) => void
}

// Una frase del texto adaptado. El mismo botón marca y quita la marca: así el foco del teclado no se pierde.
function ReviewLine({ text, comment, corrected, disabled, onToggle, onComment }: ReviewLineProps) {
  const id = useId()
  const marked = comment !== undefined
  const className = ['review-line', marked && 'review-line-marked', corrected && 'review-line-corrected']
    .filter(Boolean)
    .join(' ')
  return (
    <li className={className}>
      <p id={`${id}-text`}>
        {/* La palabra dice que ha cambiado; el color solo ayuda */}
        {corrected && <span className="review-corrected-label">Corregida: </span>}
        {text}
      </p>
      <button
        type="button"
        className="review-toggle"
        aria-describedby={`${id}-text`}
        onClick={onToggle}
        disabled={disabled}
      >
        {marked ? 'Quitar la marca' : 'Está mal'}
      </button>
      {marked && (
        <>
          <label htmlFor={`${id}-comment`}>¿Qué está mal?</label>
          <textarea
            id={`${id}-comment`}
            value={comment}
            onChange={(event) => onComment(event.target.value)}
            rows={2}
            disabled={disabled}
            autoFocus
          />
        </>
      )}
    </li>
  )
}

function countLabel(count: number, one: string, many: string): string {
  return count === 1 ? `1 ${one}` : `${count} ${many}`
}

export default Report
