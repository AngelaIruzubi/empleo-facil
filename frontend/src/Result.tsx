import type { Pictogram } from './api'
import { parseAdaptedText, type Block } from './adaptedText'

type ResultProps = {
  adaptedText: string
  pictogram: Pictogram | null
  // true mientras la IA sigue escribiendo: el texto está a medias y aún no hay pictograma
  writing: boolean
}

// El texto adaptado. La franja amarilla con el pictograma es como un cartel que dice de qué trabajo se trata.
function Result({ adaptedText, pictogram, writing }: ResultProps) {
  const { title, paragraphs } = parseAdaptedText(adaptedText)

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
        <h2 id="result-title">{title ?? 'Tu texto en lectura fácil'}</h2>
      </header>

      <div className="result-body">
        {paragraphs.map((blocks, index) => (
          <div className="result-paragraph" key={index}>
            {blocks.map((block, blockIndex) => (
              <BlockView block={block} key={blockIndex} />
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

function BlockView({ block }: { block: Block }) {
  switch (block.kind) {
    case 'heading':
      return <h3>{block.text}</h3>
    case 'sentence':
      return <p>{block.text}</p>
    case 'list':
      return (
        <ul>
          {block.items.map((item, index) => (
            <li key={index}>{item}</li>
          ))}
        </ul>
      )
  }
}

export default Result
