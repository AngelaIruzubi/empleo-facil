import { useEffect, useState, type SubmitEvent } from 'react'
import { adaptText, ApiError, MAX_TEXT_LENGTH, type EasyReadReport } from './api'
import Result from './Result'

// La pantalla siempre está en uno de estos estados
type State =
  | { status: 'idle' }
  | { status: 'loading'; startedAt: number }
  | { status: 'done'; report: EasyReadReport }
  | { status: 'error'; title: string; detail: string }

function App() {
  const [text, setText] = useState('')
  const [state, setState] = useState<State>({ status: 'idle' })

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    setState({ status: 'loading', startedAt: Date.now() })
    try {
      const report = await adaptText(text)
      setState({ status: 'done', report })
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
        <div role="status">{state.status === 'loading' && <Waiting startedAt={state.startedAt} />}</div>

        {state.status === 'error' && (
          <div className="error" role="alert">
            <h2>{state.title}</h2>
            <p>{state.detail}</p>
          </div>
        )}

        {state.status === 'done' && <Result report={state.report} />}
      </main>

      <footer>
        <p>Pictogramas: Sergio Palao. Origen: ARASAAC. Licencia: CC BY-NC-SA.</p>
      </footer>
    </>
  )
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
      <p>Tarda unos 5 minutos.</p>
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
