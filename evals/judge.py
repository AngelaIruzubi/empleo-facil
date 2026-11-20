"""La IA juez: comprueba si el texto adaptado mantiene el significado de la oferta.

Uso (con Ollama abierto):

    python evals/judge.py <nombre-de-la-prueba> [modelo-juez]

Ejemplo:

    python evals/judge.py base-gemma3-4b qwen2.5:7b

Para cada resultado de evals/resultados/<nombre>/ (y resultados-privados/ si existe), le da a la IA juez
la oferta original, el texto adaptado y los hechos clave de evals/hechos/. Para cada hecho, la juez dice:

- bien: está en el texto adaptado y significa lo mismo (aunque esté dicho con otras palabras);
- falta: el texto adaptado no lo dice;
- cambiado: lo dice, pero con otro significado (por ejemplo, «brutos» pasa a ser «sin descuentos»).

También apunta lo que el texto adaptado dice y la oferta no (información inventada).
Escribe evals/resultados/<nombre>/juez-<modelo>.json después de cada texto: si se corta, sigue donde iba.
Solo usa la librería estándar de Python.
"""

import json
import sys
import urllib.request
from pathlib import Path

EVALS_DIR = Path(__file__).parent
OLLAMA_URL = "http://localhost:11434/api/chat"
DEFAULT_JUDGE = "qwen2.5:7b"
VERDICTS = ["bien", "falta", "cambiado"]
TIMEOUT_SECONDS = 900

SYSTEM_PROMPT = """Eres una persona experta en lectura fácil y en empleo. Revisas adaptaciones de ofertas de trabajo.

Te doy tres cosas:
1. La OFERTA ORIGINAL.
2. El TEXTO ADAPTADO a lectura fácil.
3. Una lista de HECHOS CLAVE de la oferta, numerados.

Para cada hecho clave, sigue estos pasos en orden:
1. "cita": copia las frases exactas del TEXTO ADAPTADO que hablan de ese hecho. Copia todas, también las que
   dicen algo distinto. Si no hay ninguna, escribe "".
2. "diferencias": haz una lista con cada detalle del hecho que la cita no dice o dice distinto.
   Mira palabra por palabra: números, días, "brutos", "fijo discontinuo", "obligatorio" o "se valora", "el lunes se libra"...
   Si una frase de la cita contradice el hecho, también es una diferencia.
   Si la cita lo dice todo igual, aunque sea con palabras más sencillas, deja la lista vacía.
3. "veredicto": decide al final, mirando las diferencias.

Veredictos:
- "bien": el texto adaptado lo dice y significa lo mismo. Puede estar dicho con otras palabras, más sencillas.
- "falta": el texto adaptado no lo dice.
- "cambiado": el texto adaptado lo dice, pero con otro significado, o pierde un detalle que cambia lo que la persona entiende.

Ejemplos de "cambiado":
- El hecho dice "de martes a domingo" y el texto adaptado dice "todos los días".
- El hecho dice "brutos" y el texto adaptado no lo dice: la persona cree que cobrará esa cantidad entera.
- El hecho dice "se valora" o "no es obligatorio" y el texto adaptado lo pone como obligatorio.
- El hecho dice "fijo discontinuo" y el texto adaptado dice "temporal" o no explica que te llaman cada temporada.

Sé estricto: el texto adaptado es para personas con discapacidad intelectual que se fían de lo que leen.
Si en "diferencias" hay un detalle importante, el veredicto es "cambiado", no "bien".

Al final, lee el TEXTO ADAPTADO frase por frase y busca cada dato en la OFERTA ORIGINAL.
Escribe en "inventado" las frases con datos que la OFERTA ORIGINAL no dice. Ejemplo: "Te pagan más los festivos"
cuando la oferta no habla de festivos. Las explicaciones de palabras difíciles no son datos inventados.
Si no hay ninguna, deja la lista vacía.

Responde solo con el JSON pedido."""

RESPONSE_SCHEMA = {
    "type": "object",
    "properties": {
        "hechos": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "numero": {"type": "integer"},
                    "cita": {"type": "string"},
                    "diferencias": {"type": "array", "items": {"type": "string"}},
                    "veredicto": {"type": "string", "enum": VERDICTS},
                },
                "required": ["numero", "cita", "diferencias", "veredicto"],
            },
        },
        "inventado": {"type": "array", "items": {"type": "string"}},
    },
    "required": ["hechos", "inventado"],
}


def user_prompt(original: str, adapted: str, facts: list[dict]) -> str:
    numbered = "\n".join(f"{i}. {fact['dato']}" for i, fact in enumerate(facts, start=1))
    return (f"OFERTA ORIGINAL:\n{original}\n\n"
            f"TEXTO ADAPTADO:\n{adapted}\n\n"
            f"HECHOS CLAVE ({len(facts)}):\n{numbered}")


def parse_answer(answer: dict, facts: list[dict]) -> dict:
    """Une la respuesta de la juez con los hechos. Si la juez se salta un hecho, lo marca como 'sin veredicto'."""
    by_number = {item.get("numero"): item for item in answer.get("hechos", [])}
    judged = []
    for i, fact in enumerate(facts, start=1):
        item = by_number.get(i)
        verdict = item.get("veredicto") if item else None
        judged.append({
            "tipo": fact["tipo"],
            "dato": fact["dato"],
            "veredicto": verdict if verdict in VERDICTS else "sin veredicto",
            "cita": item.get("cita", "") if item else "",
            "diferencias": [x for x in item.get("diferencias", []) if x.strip()] if item else [],
        })
    return {"hechos": judged, "inventado": [x for x in answer.get("inventado", []) if x.strip()]}


def ask_judge(model: str, original: str, adapted: str, facts: list[dict]) -> dict:
    body = {
        "model": model,
        "stream": False,
        "format": RESPONSE_SCHEMA,
        "options": {"temperature": 0, "num_ctx": 8192},
        "keep_alive": "1h",
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt(original, adapted, facts)},
        ],
    }
    request = urllib.request.Request(OLLAMA_URL, data=json.dumps(body).encode("utf-8"),
                                     headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
        content = json.loads(response.read())["message"]["content"]
    return parse_answer(json.loads(content), facts)


def summary(texts: list[dict]) -> dict:
    facts = [fact for text in texts for fact in text.get("hechos", [])]
    count = {verdict: sum(1 for f in facts if f["veredicto"] == verdict) for verdict in VERDICTS + ["sin veredicto"]}
    return {
        "textos": len(texts),
        "hechos": len(facts),
        **count,
        "porcentaje_bien": round(100 * count["bien"] / len(facts), 1) if facts else 0,
        "textos_con_algo_inventado": sum(1 for text in texts if text.get("inventado")),
    }


def main() -> None:
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    name = sys.argv[1]
    model = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_JUDGE
    for results_dir, facts_dir in [(EVALS_DIR / "resultados", EVALS_DIR / "hechos"),
                                   (EVALS_DIR / "resultados-privados", EVALS_DIR / "hechos-privados")]:
        run_dir = results_dir / name
        if not run_dir.exists():
            continue
        output = run_dir / f"juez-{model.replace(':', '-')}.json"
        done = json.loads(output.read_text(encoding="utf-8"))["textos"] if output.exists() else []
        done_names = {text["texto"] for text in done}
        for path in sorted(run_dir.glob("*.json")):
            if path.name == "comprobaciones.json" or path.name.startswith("juez") or path.stem in done_names:
                continue
            result = json.loads(path.read_text(encoding="utf-8"))
            if "error" in result:
                continue
            facts = json.loads((facts_dir / path.name).read_text(encoding="utf-8"))["hechos"]
            print(f"juez · {path.stem} ...", flush=True)
            try:
                judged = ask_judge(model, result["original"], result["informe"]["adaptedText"], facts)
            except (OSError, ValueError) as error:
                # Si tarda demasiado o responde mal, sigue con el siguiente: la próxima pasada lo repite
                print(f"  ERROR: {error}", flush=True)
                continue
            done.append({"texto": path.stem, **judged})
            output.write_text(json.dumps({"juez": model, "resumen": summary(done), "textos": done},
                                         ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
            marks = " ".join(fact["veredicto"] for fact in judged["hechos"])
            print(f"  {marks}" + (f" · inventado: {len(judged['inventado'])}" if judged["inventado"] else ""),
                  flush=True)
        if output.exists():
            print(f"\n## {output.relative_to(EVALS_DIR)}\n")
            for key, value in summary(done).items():
                print(f"- {key}: {value}")


if __name__ == "__main__":
    main()
