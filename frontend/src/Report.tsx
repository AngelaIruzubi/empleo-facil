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

// Qué hacer con una frase marcada. Solo 'ai' pasa por la IA; las demás se hacen al momento.
// Angela pidió escribir la frase buena tal cual porque la IA cambiaba frases que ella ya había
// escrito bien («Dar el desayuno»), y quitar y añadir frases porque la IA no quitaba la frase
// aunque se lo pidiera en la explicación.
type Mode = 'exact' | 'ai' | 'remove' | 'add'

const MODES: { mode: Mode; label: string }[] = [
  { mode: 'exact', label: 'Cambiarla por la frase que escribo' },
  { mode: 'ai', label: 'Que la IA la corrija con mi explicación' },
  { mode: 'remove', label: 'Quitar esta frase' },
  { mode: 'add', label: 'Dejarla y añadir debajo la frase que escribo' },
]

// El título del recuadro de texto según lo que se va a escribir. Quitar no necesita texto.
const TEXT_LABELS: Record<Exclude<Mode, 'remove'>, string> = {
  exact: 'Escribe la frase buena',
  ai: 'Explica qué está mal',
  add: 'Escribe la frase que quieres añadir',
}

type Mark = {
  comment: string
  mode: Mode
}

type Revision = { status: 'idle' } | { status: 'revising' } | { status: 'error'; title: string; detail: string }

// Lo que ha pasado en el último cambio, para contárselo al preparador
type Summary = { changed: number; removed: number; added: number; unchanged: number }

// La revisión del preparador laboral: compara la oferta original con el texto adaptado,
// marca las frases que están mal y las cambia, las quita, añade frases o pide a la IA que las corrija.
// Va cerrado: la persona lectora no necesita esto. El preparador lo abre cuando quiere revisar.
// La legibilidad y las reglas no salen aquí: Angela decidió que no hacen falta para revisar.
// El servidor las sigue calculando para las evaluaciones de evals/.
function Report({ adaptedText, originalText, canRevise, onRevised }: ReportProps) {
  // Las frases marcadas: el número de línea, lo que ha escrito el preparador y qué hacer con ello
  const [marks, setMarks] = useState<Record<number, Mark>>({})
  // Las líneas nuevas o cambiadas en el último cambio, para que el preparador las revise otra vez
  const [corrected, setCorrected] = useState<number[]>([])
  const [revision, setRevision] = useState<Revision>({ status: 'idle' })
  const [summary, setSummary] = useState<Summary | null>(null)

  const lines = readingOrder(parseAdaptedText(adaptedText))
  const markedLines = Object.keys(marks)
    .map(Number)
    .sort((a, b) => a - b)
  const missingComment = markedLines.some((line) => marks[line].mode !== 'remove' && marks[line].comment.trim() === '')
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
    const byMode = (mode: Mode) => markedLines.filter((line) => marks[line].mode === mode)
    const aiLines = byMode('ai')
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
      const written = (line: number) => firstLetterUpperCase(marks[line].comment.trim())
      byMode('exact').forEach((line) => {
        if (!sameSentence(written(line), lines[line])) {
          replacements.set(line, written(line))
        }
      })
      // Una línea vacía: replaceLines la quita del texto
      byMode('remove').forEach((line) => replacements.set(line, ''))
      // La frase marcada se queda y la nueva va justo debajo
      byMode('add').forEach((line) => replacements.set(line, `${lines[line]}\n${written(line)}`))
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
      // Al añadir, la frase que se queda no es nueva: solo se resalta la añadida
      const kept = new Set(byMode('add').map((line) => revised.newLineOf[line]))
      setMarks(Object.fromEntries(unchanged.map((line) => [revised.newLineOf[line], marks[line]])))
      setCorrected(revised.changedLines.filter((line) => !kept.has(line)))
      setSummary({
        changed: byMode('exact').length + aiLines.length - unchanged.length,
        removed: byMode('remove').length,
        added: byMode('add').length,
        unchanged: unchanged.length,
      })
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
            Compara la oferta original con el texto adaptado. Si una frase está mal, pulsa «Está mal» y elige qué
            hacer: cambiarla, pedir a la IA que la corrija, quitarla o añadir una frase debajo.
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
                  : markedLines.length > 0
                    ? `Has marcado ${countLabel(markedLines.length, 'frase', 'frases')}.`
                    : // Después de un cambio ya sale el resumen: «No has marcado ninguna frase» sería ruido
                      !summary && 'No has marcado ninguna frase.'}
              </p>
              {markedLines.length > 0 && !revising && (
                <>
                  <button type="button" onClick={revise} disabled={!canRevise || missingComment}>
                    Hacer los cambios
                  </button>
                  {!canRevise && <p className="hint">Puedes hacer los cambios cuando la inteligencia artificial termine.</p>}
                  {canRevise && missingComment && (
                    <p className="hint">Escribe el texto en cada frase marcada. Solo «Quitar esta frase» no lo necesita.</p>
                  )}
                  {canRevise && !missingComment && usesAi && (
                    <p className="hint">Las frases con explicación las corrige la IA. Tarda 1 minuto por frase.</p>
                  )}
                </>
              )}
              {summary && !revising && markedLines.length === 0 && (
                <p className="review-summary" role="status">
                  {summaryMessage(summary)}
                </p>
              )}
              {summary && summary.unchanged > 0 && !revising && (
                <p className="review-unchanged" role="status">
                  {summary.unchanged === 1
                    ? 'La IA no ha cambiado 1 frase. Sigue marcada.'
                    : `La IA no ha cambiado ${summary.unchanged} frases. Siguen marcadas.`}{' '}
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
  // Es nueva o ha cambiado en el último cambio
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
        {corrected && <span className="review-corrected-label">Nueva: </span>}
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
          {/* Primero se elige qué hacer y después se escribe */}
          <fieldset className="review-mode" disabled={disabled}>
            <legend className="visually-hidden">¿Qué quieres hacer con esta frase?</legend>
            {MODES.map(({ mode, label }) => (
              <label key={mode}>
                <input
                  type="radio"
                  name={`${id}-mode`}
                  checked={mark.mode === mode}
                  onChange={() => onChange({ mode })}
                  // Al marcar la frase, el foco va a la primera opción para elegir qué hacer
                  autoFocus={mode === mark.mode}
                />
                {label}
              </label>
            ))}
          </fieldset>
          {mark.mode !== 'remove' && (
            <>
              <label htmlFor={`${id}-comment`}>{TEXT_LABELS[mark.mode]}</label>
              <textarea
                id={`${id}-comment`}
                value={mark.comment}
                onChange={(event) => onChange({ comment: event.target.value })}
                rows={2}
                disabled={disabled}
              />
            </>
          )}
        </>
      )}
    </li>
  )
}

function summaryMessage({ changed, removed, added }: Summary): string {
  const parts = [
    changed > 0 && `has cambiado ${countLabel(changed, 'frase', 'frases')}`,
    removed > 0 && `has quitado ${countLabel(removed, 'frase', 'frases')}`,
    added > 0 && `has añadido ${countLabel(added, 'frase', 'frases')}`,
  ].filter((part): part is string => Boolean(part))
  if (parts.length === 0) {
    return 'No ha cambiado ninguna frase.'
  }
  const sentence = parts.length === 1 ? parts[0] : `${parts.slice(0, -1).join(', ')} y ${parts.at(-1)}`
  return `Hecho: ${sentence}.`
}

// La frase empieza con mayúscula, aunque el preparador la escriba deprisa en minúscula
function firstLetterUpperCase(sentence: string): string {
  return sentence.charAt(0).toUpperCase() + sentence.slice(1)
}

function countLabel(count: number, one: string, many: string): string {
  return count === 1 ? `1 ${one}` : `${count} ${many}`
}

export default Report
