import { useEffect, useRef, useState } from 'react'

// Lectura en voz alta con la Web Speech API del navegador: gratis y sin instalar nada.
// Lee frase a frase para saber cuál suena (y resaltarla) y porque Chrome corta los textos largos.

// Un poco más despacio de lo normal (1): ayuda a entender
const RATE = 0.9

export type Speech = {
  // false si el navegador no sabe leer en voz alta: entonces no enseñamos el botón
  supported: boolean
  // La línea que suena ahora, o null si no está leyendo
  speakingLine: number | null
  speak: (lines: string[]) => void
  stop: () => void
}

export function useSpeech(): Speech {
  const supported = typeof window !== 'undefined' && 'speechSynthesis' in window
  const [speakingLine, setSpeakingLine] = useState<number | null>(null)
  // Cada lectura tiene un número. Si la persona pulsa Parar, los avisos de la lectura vieja se ignoran.
  const run = useRef(0)

  // Si la persona sale de la pantalla o adapta otro texto, se calla
  useEffect(() => {
    return () => {
      if (supported) {
        window.speechSynthesis.cancel()
      }
    }
  }, [supported])

  function stop() {
    run.current++
    window.speechSynthesis.cancel()
    setSpeakingLine(null)
  }

  function speak(lines: string[]) {
    stop()
    const thisRun = run.current
    const voice = spanishVoice()
    lines.forEach((line, index) => {
      const utterance = new SpeechSynthesisUtterance(line)
      utterance.lang = voice?.lang ?? 'es-ES'
      utterance.voice = voice
      utterance.rate = RATE
      utterance.onstart = () => {
        if (run.current === thisRun) setSpeakingLine(index)
      }
      utterance.onend = () => {
        if (run.current === thisRun && index === lines.length - 1) setSpeakingLine(null)
      }
      utterance.onerror = () => {
        if (run.current === thisRun) setSpeakingLine(null)
      }
      window.speechSynthesis.speak(utterance)
    })
  }

  return { supported, speakingLine, speak, stop }
}

// Mejor una voz de España. Si no hay, cualquier voz en español. Si no hay ninguna, la del navegador.
function spanishVoice(): SpeechSynthesisVoice | null {
  const voices = window.speechSynthesis.getVoices()
  return (
    voices.find((voice) => voice.lang === 'es-ES') ??
    voices.find((voice) => voice.lang.startsWith('es')) ??
    null
  )
}
