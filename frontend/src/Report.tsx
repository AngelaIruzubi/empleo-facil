import type { Change, EasyReadReport, Readability, RuleViolation } from './api'

// El informe para el preparador laboral: cómo de fácil es el texto antes y después,
// qué reglas no cumple y qué ha cambiado la IA.
// Va cerrado: la persona lectora no necesita tablas ni números. El preparador lo abre cuando quiere revisar.
function Report({ report }: { report: EasyReadReport }) {
  return (
    <details className="report">
      <summary>Informe para el preparador laboral</summary>
      <div className="report-body">
        <ReadabilitySection before={report.readabilityBefore} after={report.readabilityAfter} />
        <ViolationsSection violations={report.violations} />
        <ChangesSection changes={report.changes} />
      </div>
    </details>
  )
}

function ReadabilitySection({ before, after }: { before: Readability; after: Readability }) {
  const rows: [string, (readability: Readability) => string][] = [
    ['INFLESZ', ({ inflesz }) => `${formatNumber(inflesz.value)} (${inflesz.level})`],
    ['Fernández-Huerta', ({ fernandezHuerta }) => `${formatNumber(fernandezHuerta.value)} (${fernandezHuerta.level})`],
    ['Frases', ({ sentences }) => formatNumber(sentences)],
    ['Palabras', ({ words }) => formatNumber(words)],
    ['Palabras por frase', ({ words, sentences }) => formatNumber(words / sentences)],
  ]
  return (
    <section aria-labelledby="report-readability">
      <h3 id="report-readability">Legibilidad</h3>
      <p>
        Los dos índices van de 0 a 100. Cuanto más alto, más fácil de leer. Para lectura fácil buscamos al menos
        65 en INFLESZ, que es «bastante fácil».
      </p>
      <div className="report-table">
        <table>
          <thead>
            <tr>
              <th scope="col">Medida</th>
              <th scope="col">Oferta original</th>
              <th scope="col">Texto adaptado</th>
            </tr>
          </thead>
          <tbody>
            {rows.map(([label, value]) => (
              <tr key={label}>
                <th scope="row">{label}</th>
                <td>{value(before)}</td>
                <td>{value(after)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function ViolationsSection({ violations }: { violations: RuleViolation[] }) {
  // Primero los errores y después los avisos. sort es estable: dentro de cada grupo se mantiene el orden del texto.
  const sorted = [...violations].sort((a, b) => severityOrder(a) - severityOrder(b))
  return (
    <section aria-labelledby="report-rules">
      <h3 id="report-rules">Reglas de lectura fácil</h3>
      {sorted.length === 0 ? (
        <p>El texto adaptado cumple las {Object.keys(RULE_NAMES).length} reglas que comprueba el programa.</p>
      ) : (
        <>
          <p>
            El programa ha encontrado {countLabel(sorted.length, 'problema', 'problemas')} en el texto adaptado. Los
            errores van primero. Los avisos hay que mirarlos, pero a veces están bien.
          </p>
          <ul className="report-list">
            {sorted.map((violation, index) => (
              <li key={index}>
                <p>
                  <span className={violation.severity === 'ERROR' ? 'report-error' : 'report-warning'}>
                    {violation.severity === 'ERROR' ? 'Error' : 'Aviso'}
                  </span>{' '}
                  <strong>{RULE_NAMES[violation.rule] ?? violation.rule}:</strong>{' '}
                  {/* En la frase larga el trozo es un recuento ("17 palabras"), no texto copiado: va sin comillas */}
                  {violation.rule === 'LONG_SENTENCE' ? violation.fragment : `«${violation.fragment}»`}
                </p>
                <p className="report-quote">En la frase: «{violation.sentence}»</p>
                <p>{violation.advice}</p>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}

function ChangesSection({ changes }: { changes: Change[] }) {
  return (
    <section aria-labelledby="report-changes">
      <h3 id="report-changes">Cambios que ha hecho la IA</h3>
      <p>
        Esta lista también la escribe la IA. Puede faltar algún cambio o tener errores. Compara siempre con la oferta
        original.
      </p>
      {changes.length === 0 ? (
        <p>La IA no ha apuntado ningún cambio.</p>
      ) : (
        <ol className="report-list">
          {changes.map((change, index) => (
            <li key={index}>
              <dl>
                <dt>Antes</dt>
                {/* A veces la IA añade una explicación que no estaba en la oferta: el original llega vacío */}
                <dd>{change.original.trim() === '' ? 'Nada. Es texto nuevo.' : `«${change.original}»`}</dd>
                <dt>Después</dt>
                <dd>«{change.adapted}»</dd>
                <dt>Por qué</dt>
                <dd>{change.reason}</dd>
              </dl>
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}

// Los nombres de las reglas de EasyReadRule.java. Si llega una regla nueva sin nombre aquí, se enseña tal cual.
const RULE_NAMES: Record<string, string> = {
  LONG_SENTENCE: 'Frase larga',
  FORBIDDEN_SIGN: 'Signo que hay que evitar',
  PERCENTAGE: 'Porcentaje',
  ORDINAL: 'Número ordinal',
  DATE_WITH_SLASHES: 'Fecha con barras',
  MENTE_WORD: 'Palabra terminada en -mente',
  ISIMO_WORD: 'Palabra terminada en -ísimo',
  ACRONYM: 'Sigla',
  VAGUE_WORD: 'Palabra poco clara',
  SLASH: 'Barra',
}

function severityOrder({ severity }: RuleViolation): number {
  return severity === 'ERROR' ? 0 : 1
}

function countLabel(count: number, one: string, many: string): string {
  return count === 1 ? `1 ${one}` : `${count} ${many}`
}

// Con coma decimal, como se escribe en España: 68,5
function formatNumber(value: number): string {
  return value.toLocaleString('es-ES', { maximumFractionDigits: 1 })
}

export default Report
