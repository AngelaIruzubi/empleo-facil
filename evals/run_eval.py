"""Pasa los textos de prueba por la web, igual que una persona usuaria, y guarda el informe de cada uno.

Uso (con el backend arrancado):

    python evals/run_eval.py <nombre-de-la-prueba> [url-del-backend]

Ejemplo:

    python evals/run_eval.py base-gemma3-4b http://localhost:8081

Lee evals/textos/ y, si existe, evals/textos-privados/ (ofertas reales que no se suben a GitHub).
Guarda un JSON por texto en evals/resultados/<nombre-de-la-prueba>/ (o resultados-privados/ para los privados).
Si un texto ya tiene su JSON, lo salta: si la pasada se corta, se vuelve a lanzar y sigue donde iba.
Solo usa la librería estándar de Python.
"""

import json
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

EVALS_DIR = Path(__file__).parent
SOURCES = [
    (EVALS_DIR / "textos", EVALS_DIR / "resultados"),
    (EVALS_DIR / "textos-privados", EVALS_DIR / "resultados-privados"),
]
DEFAULT_BACKEND = "http://localhost:8080"
TIMEOUT_SECONDS = 900


def adapt(backend: str, text: str) -> dict:
    """Llama a /api/easy-read/stream y devuelve el informe y los tiempos que ve la persona."""
    request = urllib.request.Request(
        backend + "/api/easy-read/stream",
        data=json.dumps({"text": text}).encode("utf-8"),
        headers={"Content-Type": "application/json; charset=utf-8"},
    )
    start = time.perf_counter()
    seconds = lambda: round(time.perf_counter() - start, 1)
    timings = {"pictograma_s": None, "primer_texto_s": None, "texto_completo_s": None}
    event = None
    with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
        for raw in response:
            line = raw.decode("utf-8").rstrip("\r\n")
            if line.startswith("event:"):
                event = line[len("event:"):].strip()
            elif line.startswith("data:"):
                data = line[len("data:"):]
                if event == "pictogram" and timings["pictograma_s"] is None:
                    timings["pictograma_s"] = seconds()
                elif event == "text" and timings["primer_texto_s"] is None:
                    timings["primer_texto_s"] = seconds()
                elif event == "text-end":
                    timings["texto_completo_s"] = seconds()
                elif event == "report":
                    return {"segundos": seconds(), **timings, "informe": json.loads(data)}
                elif event == "error":
                    return {"segundos": seconds(), **timings, "error": json.loads(data)}
    return {"segundos": seconds(), **timings, "error": {"detail": "El stream terminó sin informe"}}


def main() -> None:
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    name = sys.argv[1]
    backend = (sys.argv[2] if len(sys.argv) > 2 else DEFAULT_BACKEND).rstrip("/")

    for texts_dir, results_dir in SOURCES:
        if not texts_dir.exists():
            continue
        output_dir = results_dir / name
        output_dir.mkdir(parents=True, exist_ok=True)
        for text_file in sorted(texts_dir.glob("*.txt")):
            output = output_dir / (text_file.stem + ".json")
            if output.exists():
                continue
            print(f"{name} · {text_file.name} ...", flush=True)
            text = text_file.read_text(encoding="utf-8").strip()
            try:
                result = adapt(backend, text)
            except (urllib.error.URLError, TimeoutError) as error:
                result = {"segundos": None, "error": {"detail": str(error)}}
            result = {"prueba": name, "texto": text_file.stem, "original": text, **result}
            output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
            status = "ERROR" if "error" in result else "listo"
            print(f"  {status} en {result['segundos']} s", flush=True)


if __name__ == "__main__":
    main()
