import type { EasyReadReport } from './api'
import { parseAdaptedText, type Block } from './adaptedText'

type ResultProps = {
  report: EasyReadReport
}

// El texto adaptado. La franja amarilla con el pictograma es como un cartel que dice de qué trabajo se trata.
function Result({ report }: ResultProps) {
  const { title, paragraphs } = parseAdaptedText(report.adaptedText)

  return (
    <article className="result" aria-labelledby="result-title">
      <header className="result-sign">
        {report.pictogram && (
          <img
            className="result-pictogram"
            src={report.pictogram.imageUrl}
            alt={report.pictogram.keyword}
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

      <p className="result-notice">
        Este texto lo ha adaptado una inteligencia artificial.
        <br />
        Una persona tiene que revisarlo.
      </p>
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
