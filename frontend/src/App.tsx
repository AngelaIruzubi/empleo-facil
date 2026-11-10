import { useEffect, useState } from 'react'

type BackendStatus = 'checking' | 'connected' | 'disconnected'

const statusMessages: Record<BackendStatus, string> = {
  checking: 'Estamos comprobando la conexión con el servidor.',
  connected: 'La web está conectada con el servidor.',
  disconnected: 'La web no puede conectar con el servidor. Arranca el backend y recarga la página.',
}

function App() {
  const [status, setStatus] = useState<BackendStatus>('checking')

  // Se ejecuta una vez, al abrir la página
  useEffect(() => {
    fetch('/health')
      .then((response) => setStatus(response.ok ? 'connected' : 'disconnected'))
      .catch(() => setStatus('disconnected'))
  }, [])

  return (
    <main>
      <h1>Empleo fácil</h1>
      <p>{statusMessages[status]}</p>
    </main>
  )
}

export default App
