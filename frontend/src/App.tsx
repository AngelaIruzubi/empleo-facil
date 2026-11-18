import { useEffect, useState, type SubmitEvent } from 'react'
import { adaptTextStreaming, ApiError, MAX_TEXT_LENGTH, type EasyReadReport } from './api'
import Report from './Report'
import Result, { type ResultStage } from './Result'

// La pantalla siempre está en uno de estos estados.
// En 'loading', textSoFar es el texto que la IA ha escrito hasta ahora (vacío al principio)
// y textComplete dice si ya ha terminado el texto (falta la lista de cambios y el pictograma).
type State =
  | { status: 'idle' }
  | { status: 'loading'; startedAt: number; textSoFar: string; textComplete: boolean }
  | { status: 'done'; report: EasyReadReport; originalText: string }
  | { status: 'error'; title: string; detail: string }

function App() {
  const [text, setText] = useState('')
  const [state, setState] = useState<State>({ status: 'idle' })

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const startedAt = Date.now()
    // La oferta que se adapta. Si la persona cambia el cuadro de texto después, el informe sigue con esta.
    const originalText = text
    setState({ status: 'loading', startedAt, textSoFar: '', textComplete: false })
    try {
      const report = await adaptTextStreaming(
        originalText,
        (textSoFar) => setState({ status: 'loading', startedAt, textSoFar, textComplete: false }),
        () => setState((current) => (current.status === 'loading' ? { ...current, textComplete: true } : current)),
      )
      setState({ status: 'done', report, originalText })
    } catch (error) {
      if (error instanceof ApiError) {
        setState({ status: 'error', title: error.title, detail: error.detail })
      } else {
        setState({
          status: 'error',
          title: 'No se puede adaptar el texto ahora',
          detail: 'Ahora no podemos adaptar el texto.\nInténtalo otra vez dentro de un rato.',
        })
      }
    }
  }

  const loading = state.status === 'loading'
  const result = resultToShow(state)

  return (
    <>
      <main>
        <h1>Empleo fácil</h1>
        <p className="intro">Pega una oferta de trabajo y te la explicamos con palabras fáciles.</p>

        <form onSubmit={handleSubmit}>
          <label htmlFor="text">Texto de la oferta</label>
          <textarea
            id="text"
            value={text}
            onChange={(event) => setText(event.target.value)}
            maxLength={MAX_TEXT_LENGTH}
            rows={8}
            required
            aria-describedby="text-hint"
          />
          <p id="text-hint" className="hint">
            Puedes pegar {MAX_TEXT_LENGTH} letras como máximo. Llevas {text.length}.
          </p>
          <button type="submit" disabled={loading || text.trim() === ''}>
            Adaptar el texto
          </button>
        </form>

        {/* role="status" y role="alert" hacen que un lector de pantalla lea el mensaje en voz alta */}
        <div role="status">
          {state.status === 'loading' && state.textSoFar === '' && <Waiting startedAt={state.startedAt} />}
          {result?.stage === 'writing' && (
            <p className="visually-hidden">Ya sale el texto. La inteligencia artificial sigue escribiendo.</p>
          )}
          {result && result.stage !== 'writing' && (
            <p className="visually-hidden">Ya está tu texto en lectura fácil.</p>
          )}
        </div>

        {/* Un solo Result en un solo sitio: así no se borra (ni se corta la lectura en voz alta) al llegar el informe */}
        {result && <Result {...result} />}

        {state.status === 'done' && <Report report={state.report} originalText={state.originalText} />}

        {state.status === 'error' && (
          <div className="error" role="alert">
            <h2>{state.title}</h2>
            <p>{state.detail}</p>
          </div>
        )}
      </main>

      <footer>
        <p>Pictogramas: Sergio Palao. Origen: ARASAAC. Licencia: CC BY-NC-SA.</p>
      </footer>
    </>
  )
}

// Qué texto enseñar y en qué momento está: escribiendo, terminando (falta el pictograma) o terminado
function resultToShow(state: State) {
  if (state.status === 'loading' && state.textSoFar !== '') {
    const stage: ResultStage = state.textComplete ? 'finishing' : 'writing'
    return { adaptedText: state.textSoFar, pictogram: null, stage }
  }
  if (state.status === 'done') {
    const stage: ResultStage = 'done'
    return { adaptedText: state.report.adaptedText, pictogram: state.report.pictogram, stage }
  }
  return null
}

// La IA tarda unos minutos en este ordenador. Enseñamos el tiempo real, sin barras de progreso inventadas.
function Waiting({ startedAt }: { startedAt: number }) {
  const [minutes, setMinutes] = useState(0)

  useEffect(() => {
    const timer = setInterval(() => setMinutes(Math.floor((Date.now() - startedAt) / 60_000)), 15_000)
    return () => clearInterval(timer)
  }, [startedAt])

  return (
    <div className="waiting">
      <h2>Estamos adaptando tu texto</h2>
      <p>El texto empieza a salir en unos segundos.</p>
      <p>Si es la primera vez, puede tardar 2 minutos.</p>
      <p>No cierres esta página.</p>
      <p>{elapsedMessage(minutes)}</p>
    </div>
  )
}

function elapsedMessage(minutes: number): string {
  if (minutes === 0) {
    return 'Acabas de empezar.'
  }
  if (minutes === 1) {
    return 'Llevas 1 minuto esperando.'
  }
  return `Llevas ${minutes} minutos esperando.`
}

export default App
