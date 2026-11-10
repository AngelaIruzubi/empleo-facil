# Frontend de Empleo fácil

Web hecha con React, TypeScript y Vite.

## Arrancar en local

1. Arranca el backend (carpeta `backend/`).
2. Si el backend no usa el puerto 8080, copia `.env.example` como `.env.local` y cambia `BACKEND_URL`.
3. Instala las dependencias y arranca la web:

```bash
npm install
npm run dev
```

La web se abre en http://localhost:5173.

## Comandos

| Comando | Qué hace |
|---|---|
| `npm run dev` | Arranca la web y la recarga al guardar un cambio |
| `npm run build` | Comprueba los tipos y prepara la web para publicarla en `dist/` |
| `npm run lint` | Busca errores comunes en el código |
