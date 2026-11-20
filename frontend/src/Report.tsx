import { useId, useState } from 'react'
import { parseAdaptedText, readingOrder, replaceLines, sameSentence } from './adaptedText'
import { ApiError, reviseSentences } from './api'

type ReportProps = {
  // El texto adaptado completo
  adaptedText: string
  // La oferta tal como la pegó la persona
  originalText: string
  // Se puede cambiar el texto cuando la IA ha terminado del todo (si no, el informe que llega lo pisaría)
  canRevise: boolean
  // Avisa del texto nuevo con las frases cambiadas, para enseñarlo arriba y leerlo con «Escuchar»
  onRevised: (adaptedText: string) => void
}

// exact: el preparador ha escrito la frase buena y se pone tal cual, sin la IA.
// ai: el preparador ha explicado qué está mal y la IA corrige la frase.
// Angela lo pidió así porque la IA cambiaba frases que ella ya había escrito bien («Dar el desayuno»).
type Mode = 'exact' | 'ai'

type Mark = {
  comment: string
  mode: Mode
}

type Revision = { status: 'idle' } | { status: 'revising' } | { status: 'error'; title: string; detail: string }

// La revisión del preparador laboral: compara la oferta original con el texto adaptado,
// marca las frases que están mal y escribe la frase buena o pide a la IA que la corrija.
// Va cerrado: la persona lectora no necesita esto. El preparador lo abre cuando quiere revisar.
// La legibilidad y las reglas no salen aquí: Angela decidió que no hacen falta para revisar.
// El servidor las sigue calculando para las evaluaciones de evals/.
function Report({ adaptedText, originalText, canRevise, onRevised }: ReportProps) {
  // Las frases marcadas: el número de línea, lo que ha escrito el preparador y qué hacer con ello
  const [marks, setMarks] = useState<Record<number, Mark>>({})
  // Las líneas que ha cambiado el último cambio, para que el preparador las revise otra vez
  const [corrected, setCorrected] = useState<number[]>([])
  const [revision, setRevision] = useState<Revision>({ status: 'idle' })
  // Cuántas frases ha devuelto la IA sin cambiar en la última corrección
  const [unchangedCount, setUnchangedCount] = useState(0)

  const lines = readingOrder(parseAdaptedText(adaptedText))
  const markedLines = Object.keys(marks)
    .map(Number)
    .sort((a, b) => a - b)
  const missingComment = markedLines.some((line) => marks[line].comment.trim() === '')
  const usesAi = markedLines.some((line) => marks[line].mode === 'ai')
  const revising = revision.status === 'revising'

  function toggle(line: number) {
    setMarks(({ [line]: current, ...others }) =>
      current === undefined ? { ...others, [line]: { comment: '', mode: 'exact' } } : others,
    )
  }

  function updateMark(line: number, change: Partial<Mark>) {
    setMarks((current) => ({ ...current, [line]: { ...current[line], ...change } }))
  }

  async function revise() {
    const exactLines = markedLines.filter((line) => marks[line].mode === 'exact')
    const aiLines = markedLines.filter((line) => marks[line].mode === 'ai')
    setRevision(aiLines.length > 0 ? { status: 'revising' } : { status: 'idle' })
    try {
      // Solo se llama a la IA si hay alguna frase con explicación
      const aiSentences =
        aiLines.length === 0
          ? []
          : await reviseSentences(
              originalText,
              adaptedText,
              aiLines.map((line) => ({ sentence: lines[line], comment: marks[line].comment })),
            )

      const replacements = new Map<number, string>()
      const unchanged: number[] = []
      exactLines.forEach((line) => {
        const sentence = firstLetterUpperCase(marks[line].comment.trim())
        if (!sameSentence(sentence, lines[line])) {
          replacements.set(line, sentence)
        }
      })
      // Si la IA ha devuelto la misma frase, no la cambiamos: se queda marcada
      // para que el preparador lo intente otra vez, lo explique de otra forma o escriba la frase
      aiLines.forEach((line, index) => {
        if (sameSentence(aiSentences[index], lines[line])) {
          unchanged.push(line)
        } else {
          replacements.set(line, aiSentences[index])
        }
      })

      const revised = replaceLines(adaptedText, replacements)
      setMarks(Object.fromEntries(unchanged.map((line) => [revised.newLineOf[line], marks[line]])))
      setCorrected(revised.changedLines)
      setUnchangedCount(unchanged.length)
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
          <p>
            Compara la oferta original con el texto adaptado. Si una frase está mal, pulsa «Está mal». Después escribe
            la frase buena o explica qué falla.
          </p>
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
                  mark={marks[line]}
                  corrected={corrected.includes(line)}
                  disabled={revising}
                  onToggle={() => toggle(line)}
                  onChange={(change) => updateMark(line, change)}
                />
              ))}
            </ol>

            <div className="review-actions">
              <p className="review-count" role="status">
                {revising
                  ? 'La inteligencia artificial está corrigiendo las frases. Puede tardar 1 minuto por frase.'
                  : markedLines.length === 0
                    ? 'No has marcado ninguna frase.'
                    : `Has marcado ${countLabel(markedLines.length, 'frase', 'frases')}.`}
              </p>
              {markedLines.length > 0 && !revising && (
                <>
                  <button type="button" onClick={revise} disabled={!canRevise || missingComment}>
                    Cambiar {markedLines.length === 1 ? 'la frase marcada' : 'las frases marcadas'}
                  </button>
                  {!canRevise && <p className="hint">Puedes cambiarlas cuando la inteligencia artificial termine.</p>}
                  {canRevise && missingComment && (
                    <p className="hint">Escribe la frase buena o qué está mal en cada frase marcada.</p>
                  )}
                  {canRevise && !missingComment && usesAi && (
                    <p className="hint">Las frases con explicación las corrige la IA. Tarda 1 minuto por frase.</p>
                  )}
                </>
              )}
              {unchangedCount > 0 && !revising && (
                <p className="review-unchanged" role="status">
                  {unchangedCount === 1
                    ? 'La IA no ha cambiado 1 frase. Sigue marcada.'
                    : `La IA no ha cambiado ${unchangedCount} frases. Siguen marcadas.`}{' '}
                  Prueba otra vez o escribe tú la frase buena.
                </p>
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
  mark: Mark | undefined
  // La ha cambiado el último cambio
  corrected: boolean
  // Mientras la IA corrige no se pueden cambiar las marcas
  disabled: boolean
  onToggle: () => void
  onChange: (change: Partial<Mark>) => void
}

// Una frase del texto adaptado. El mismo botón marca y quita la marca: así el foco del teclado no se pierde.
function ReviewLine({ text, mark, corrected, disabled, onToggle, onChange }: ReviewLineProps) {
  const id = useId()
  const className = ['review-line', mark && 'review-line-marked', corrected && 'review-line-corrected']
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
        {mark ? 'Quitar la marca' : 'Está mal'}
      </button>
      {mark && (
        <>
          {/* Primero se elige qué se va a escribir y después se escribe */}
          <fieldset className="review-mode" disabled={disabled}>
            <legend className="visually-hidden">¿Qué has escrito?</legend>
            <label>
              <input
                type="radio"
                name={`${id}-mode`}
                checked={mark.mode === 'exact'}
                onChange={() => onChange({ mode: 'exact' })}
              />
              La frase buena: ponla tal cual
            </label>
            <label>
              <input
                type="radio"
                name={`${id}-mode`}
                checked={mark.mode === 'ai'}
                onChange={() => onChange({ mode: 'ai' })}
              />
              Una explicación: que la IA corrija la frase
            </label>
          </fieldset>
          <label htmlFor={`${id}-comment`}>
            {mark.mode === 'exact' ? 'Escribe la frase buena' : 'Explica qué está mal'}
          </label>
          <textarea
            id={`${id}-comment`}
            value={mark.comment}
            onChange={(event) => onChange({ comment: event.target.value })}
            rows={2}
            disabled={disabled}
            autoFocus
          />
        </>
      )}
    </li>
  )
}

// La frase buena empieza con mayúscula, aunque el preparador la escriba deprisa en minúscula
function firstLetterUpperCase(sentence: string): string {
  return sentence.charAt(0).toUpperCase() + sentence.slice(1)
}

function countLabel(count: number, one: string, many: string): string {
  return count === 1 ? `1 ${one}` : `${count} ${many}`
}

export default Report
