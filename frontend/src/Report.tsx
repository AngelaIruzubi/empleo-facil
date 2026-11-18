import { useId, useState } from 'react'
import { parseAdaptedText, readingOrder } from './adaptedText'

type ReportProps = {
  // El texto adaptado completo
  adaptedText: string
  // La oferta tal como la pegó la persona
  originalText: string
}

// La revisión del preparador laboral: compara la oferta original con el texto adaptado
// y marca las frases que están mal, con un comentario para la IA.
// Va cerrado: la persona lectora no necesita esto. El preparador lo abre cuando quiere revisar.
// La legibilidad y las reglas no salen aquí: Angela decidió que no hacen falta para revisar.
// El servidor las sigue calculando para las evaluaciones de evals/.
function Report({ adaptedText, originalText }: ReportProps) {
  // Las frases marcadas: el número de línea y lo que ha escrito el preparador
  const [comments, setComments] = useState<Record<number, string>>({})
  const lines = readingOrder(parseAdaptedText(adaptedText))
  const marked = Object.keys(comments).length

  function toggle(line: number) {
    setComments(({ [line]: current, ...others }) => (current === undefined ? { ...others, [line]: '' } : others))
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
                  onToggle={() => toggle(line)}
                  onComment={(comment) => setComments((current) => ({ ...current, [line]: comment }))}
                />
              ))}
            </ol>
            <p className="review-count" role="status">
              {marked === 0 ? 'No has marcado ninguna frase.' : `Has marcado ${countLabel(marked, 'frase', 'frases')}.`}
            </p>
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
  onToggle: () => void
  onComment: (comment: string) => void
}

// Una frase del texto adaptado. El mismo botón marca y quita la marca: así el foco del teclado no se pierde.
function ReviewLine({ text, comment, onToggle, onComment }: ReviewLineProps) {
  const id = useId()
  const marked = comment !== undefined
  return (
    <li className={marked ? 'review-line review-line-marked' : 'review-line'}>
      <p id={`${id}-text`}>{text}</p>
      <button type="button" className="review-toggle" aria-describedby={`${id}-text`} onClick={onToggle}>
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
