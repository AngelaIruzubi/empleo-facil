// Lanza Vitest (npm test) desde la carpeta frontend con la letra de la unidad en mayúscula.
//
// En Windows, VS Code abre la carpeta como c:\ (minúscula) y Node a veces la escribe como C:\.
// Vitest cargaba entonces dos veces el mismo módulo y fallaba con "failed to find the current suite".
// En Linux (GitHub Actions) no hay letras de unidad y no cambia nada.
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const frontend = path
  .resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
  .replace(/^[a-z](?=:)/, (drive) => drive.toUpperCase())
const vitest = path.join(frontend, 'node_modules', 'vitest', 'vitest.mjs')

const { status } = spawnSync(process.execPath, [vitest, 'run', ...process.argv.slice(2)], {
  cwd: frontend,
  stdio: 'inherit',
})
process.exit(status ?? 1)
