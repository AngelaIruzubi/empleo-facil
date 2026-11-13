import type { Pictogram } from './api'
import { parseAdaptedText, readingOrder, type Block } from './adaptedText'
import { useSpeech } from './useSpeech'

type ResultProps = {
  adaptedText: string
  pictogram: Pictogram | null
  // true mientras la IA sigue escribiendo: el texto está a medias y aún no hay pictograma
  writing: boolean
}

// El texto adaptado. La franja amarilla con el pictograma es como un cartel que dice de qué trabajo se trata.
function Result({ adaptedText, pictogram, writing }: ResultProps) {
  const parsed = parseAdaptedText(adaptedText)
  const { title, paragraphs } = parsed
  const speech = useSpeech()

  const numbered = numberLines(title, paragraphs)
  const speaking = speech.speakingLine

  return (
    // aria-busy: el lector de pantalla espera a que el texto esté completo para leerlo
    <article className="result" aria-labelledby="result-title" aria-busy={writing}>
      <header className="result-sign">
        {pictogram && (
          <img
            className="result-pictogram"
            src={pictogram.imageUrl}
            alt={pictogram.keyword}
            width={120}
            height={120}
          />
        )}
        <h2 id="result-title">
          <span className={title && speaking === 0 ? 'speaking' : undefined}>
            {title ?? 'Tu texto en lectura fácil'}
          </span>
        </h2>
      </header>

      {!writing && speech.supported && (
        <button
          type="button"
          className="listen"
          onClick={() => (speaking === null ? speech.speak(readingOrder(parsed)) : speech.stop())}
        >
          <SpeakerIcon />
          {speaking === null ? 'Escuchar el texto' : 'Parar'}
        </button>
      )}

      <div className="result-body">
        {numbered.map((blocks, index) => (
          <div className="result-paragraph" key={index}>
            {blocks.map(({ block, firstLine }, blockIndex) => (
              <BlockView block={block} firstLine={firstLine} speaking={speaking} key={blockIndex} />
            ))}
          </div>
        ))}
      </div>

      {writing ? (
        <p className="result-writing">La inteligencia artificial sigue escribiendo.</p>
      ) : (
        <p className="result-notice">
          Este texto lo ha adaptado una inteligencia artificial.
          <br />
          Una persona tiene que revisarlo.
        </p>
      )}
    </article>
  )
}

// Numera las líneas en el orden de lectura (el mismo que readingOrder) para resaltar la que suena.
// La línea 0 es el título, si lo hay.
function numberLines(title: string | null, paragraphs: Block[][]) {
  let nextLine = title ? 1 : 0
  return paragraphs.map((blocks) =>
    blocks.map((block) => {
      const firstLine = nextLine
      nextLine += block.kind === 'list' ? block.items.length : 1
      return { block, firstLine }
    }),
  )
}

type BlockViewProps = {
  block: Block
  // El número de su primera línea en el orden de lectura
  firstLine: number
  speaking: number | null
}

function BlockView({ block, firstLine, speaking }: BlockViewProps) {
  const highlight = (line: number) => (line === speaking ? 'speaking' : undefined)
  switch (block.kind) {
    case 'heading':
      return <h3 className={highlight(firstLine)}>{block.text}</h3>
    case 'sentence':
      return <p className={highlight(firstLine)}>{block.text}</p>
    case 'list':
      return (
        <ul>
          {block.items.map((item, index) => (
            <li className={highlight(firstLine + index)} key={index}>
              {item}
            </li>
          ))}
        </ul>
      )
  }
}

// Un altavoz. Acompaña a la palabra, no la sustituye: el botón siempre dice qué hace.
function SpeakerIcon() {
  return (
    <svg className="listen-icon" viewBox="0 0 24 24" width="28" height="28" aria-hidden="true" focusable="false">
      <path d="M4 9h4l5-4v14l-5-4H4z" fill="currentColor" />
      <path
        d="M16 8.5a5 5 0 0 1 0 7M18.5 6a8.5 8.5 0 0 1 0 12"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
      />
    </svg>
  )
}

export default Result
