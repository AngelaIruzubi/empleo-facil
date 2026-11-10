import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// Por seguridad, el navegador no deja que la web llame a una API de otra dirección (CORS).
// Mientras programamos, Vite hace de intermediario: pasa al backend las llamadas a /api y /health.
// La dirección del backend se cambia con BACKEND_URL en el archivo .env.local
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendUrl = env.BACKEND_URL ?? 'http://localhost:8080'

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': backendUrl,
        '/health': backendUrl,
      },
    },
  }
})
