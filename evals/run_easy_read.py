"""Pasa los textos de evals/textos/ por el motor de lectura fácil y guarda los resultados.

Uso (con el backend arrancado y el modelo configurado en OLLAMA_MODEL):

    python evals/run_easy_read.py <nombre-del-modelo> [url-del-endpoint]

Ejemplo:

    python evals/run_easy_read.py gemma3-4b http://localhost:8081/api/easy-read

Los resultados se guardan en evals/resultados/<nombre-del-modelo>/.
Solo usa la librería estándar de Python.
"""

import json
import sys
import time
import urllib.request
from pathlib import Path

EVALS_DIR = Path(__file__).parent
TEXTS_DIR = EVALS_DIR / "textos"
RESULTS_DIR = EVALS_DIR / "resultados"
DEFAULT_URL = "http://localhost:8080/api/easy-read"
TIMEOUT_SECONDS = 600


def adapt(url: str, text: str) -> str:
    body = json.dumps({"text": text}).encode("utf-8")
    request = urllib.request.Request(
        url, data=body, headers={"Content-Type": "application/json; charset=utf-8"}
    )
    with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
        return json.load(response)["adaptedText"]


def main() -> None:
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    model = sys.argv[1]
    url = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_URL

    output_dir = RESULTS_DIR / model
    output_dir.mkdir(parents=True, exist_ok=True)

    for text_file in sorted(TEXTS_DIR.glob("*.txt")):
        print(f"{model} · {text_file.name} ...", flush=True)
        start = time.perf_counter()
        adapted = adapt(url, text_file.read_text(encoding="utf-8").strip())
        seconds = time.perf_counter() - start

        header = f"# Modelo: {model}\n# Texto: {text_file.name}\n# Tiempo: {seconds:.0f} s\n\n"
        (output_dir / text_file.name).write_text(header + adapted + "\n", encoding="utf-8", newline="\n")
        print(f"  listo en {seconds:.0f} s", flush=True)


if __name__ == "__main__":
    main()
