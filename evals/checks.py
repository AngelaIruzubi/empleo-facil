"""Comprobaciones con código, sin IA, de los resultados de run_eval.py.

Uso:

    python evals/checks.py <nombre-de-la-prueba>

Lee evals/resultados/<nombre>/*.json (y resultados-privados/ si existe) y los hechos de evals/hechos/.
Escribe evals/resultados/<nombre>/comprobaciones.json y enseña un resumen.

Comprueba:
- números: qué cifras, horas e importes de la oferta no aparecen en el texto adaptado;
- días de la semana: si el texto adaptado pone un día de la semana que no corresponde a la fecha;
- pictograma: que sea de una profesión válida y que no salga en lo que no es una oferta;
- legibilidad y reglas: INFLESZ de 65 o más y los fallos de las reglas (los calcula el backend).

Es generoso a propósito: si duda, da el número por bueno. Lo que marca como perdido suele ser un fallo de verdad.
"""

import datetime
import json
import re
import sys
from pathlib import Path

EVALS_DIR = Path(__file__).parent
WEEKDAYS = ["lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"]
MONTHS = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
          "septiembre", "octubre", "noviembre", "diciembre"]
NUMBER_WORDS = {"un": 1, "uno": 1, "una": 1, "dos": 2, "tres": 3, "cuatro": 4, "cinco": 5, "seis": 6,
                "siete": 7, "ocho": 8, "nueve": 9, "diez": 10, "once": 11, "doce": 12, "quince": 15,
                "veinte": 20, "treinta": 30, "media": None}
EASY_READ_MIN_INFLESZ = 65

NUMBER = re.compile(r"\d+(?:[.,:]\d+)*")
DATE_WITH_WEEKDAY = re.compile(
    r"\b(" + "|".join(WEEKDAYS) + r"),?\s+(\d{1,2})\s+de\s+(" + "|".join(MONTHS) + r")(?:\s+de\s+(\d{4}))?",
    re.IGNORECASE)


def numbers_in(text: str) -> tuple[set[str], set[tuple[int, int]]]:
    """Los números de un texto, escritos siempre igual: '1.300' y '1300' son '1300'; '9,80' y '9.80' son '9,80'.
    Las horas van aparte, como (hora, minutos)."""
    plain, times = set(), set()
    for token in NUMBER.findall(text):
        if ":" in token:
            parts = token.split(":")
            if len(parts) == 2 and len(parts[1]) == 2:
                times.add((int(parts[0]), int(parts[1])))
                continue
        if re.fullmatch(r"\d{1,3}(?:\.\d{3})+(?:,\d+)?", token):
            token = token.replace(".", "")
        elif re.fullmatch(r"\d+\.\d{1,2}", token):
            token = token.replace(".", ",")
        plain.add(token)
    for word in re.findall(r"\b\w+\b", text.lower()):
        if NUMBER_WORDS.get(word):
            plain.add(str(NUMBER_WORDS[word]))
    return plain, times


def lost_numbers(original: str, adapted: str) -> list[str]:
    """Las cifras y horas de la oferta que no están en el texto adaptado."""
    original_plain, original_times = numbers_in(original)
    adapted_plain, adapted_times = numbers_in(adapted)
    # Una hora del texto adaptado también sirve como número suelto: '8:00' cuenta como '8'
    adapted_plain |= {str(hour) for hour, _ in adapted_times}
    lost = sorted(number for number in original_plain if number not in adapted_plain)
    for hour, minutes in sorted(original_times):
        found = (hour, minutes) in adapted_times
        if not found and minutes == 0:
            # '17:00' puede ir como '17', '5 de la tarde' o 'las 5'
            found = str(hour) in adapted_plain or (hour > 12 and str(hour - 12) in adapted_plain)
        if not found:
            lost.append(f"{hour}:{minutes:02d}")
    return lost


def wrong_weekdays(adapted: str, default_year: int) -> list[str]:
    """Fechas del texto adaptado con un día de la semana que no es el suyo. Ejemplo: 'lunes 1 de diciembre de 2026'."""
    wrong = []
    for weekday, day, month, year in DATE_WITH_WEEKDAY.findall(adapted):
        try:
            date = datetime.date(int(year or default_year), MONTHS.index(month.lower()) + 1, int(day))
        except ValueError:
            wrong.append(f"{weekday} {day} de {month}: la fecha no existe")
            continue
        real = WEEKDAYS[date.weekday()]
        if real != weekday.lower():
            wrong.append(f"{weekday} {day} de {month} de {date.year} es {real}")
    return wrong


def pictogram_check(report: dict, facts: dict) -> str:
    """'bien', 'sin pictograma', 'pictograma equivocado: x' o 'pictograma de más: x'."""
    pictogram = report.get("pictogram")
    keyword = pictogram["keyword"].lower() if pictogram else None
    if not facts["oferta"]:
        return "bien" if keyword is None else f"pictograma de más: {keyword}"
    if keyword is None:
        return "sin pictograma"
    valid = {profession.lower() for profession in facts["profesiones_validas"]}
    return "bien" if keyword in valid else f"pictograma equivocado: {keyword}"


def check(result: dict, facts: dict) -> dict:
    if "error" in result:
        return {"texto": result["texto"], "error": result["error"].get("detail", "error")}
    report = result["informe"]
    original, adapted = result["original"], report["adaptedText"]
    years = [int(year) for year in re.findall(r"\b(20\d\d)\b", original)]
    violations = report.get("violations", [])
    return {
        "texto": result["texto"],
        "segundos": result["segundos"],
        "inflesz_antes": report["readabilityBefore"]["inflesz"]["value"],
        "inflesz_despues": report["readabilityAfter"]["inflesz"]["value"],
        "lectura_facil": report["readabilityAfter"]["inflesz"]["value"] >= EASY_READ_MIN_INFLESZ,
        "errores_reglas": sum(1 for v in violations if v["severity"] == "ERROR"),
        "avisos_reglas": sum(1 for v in violations if v["severity"] == "WARNING"),
        "numeros_perdidos": lost_numbers(original, adapted),
        "dias_equivocados": wrong_weekdays(adapted, years[0] if years else 2027),
        "pictograma": pictogram_check(report, facts),
    }


def summary(checks: list[dict]) -> dict:
    ok = [c for c in checks if "error" not in c]
    count = len(ok) or 1
    return {
        "textos": len(checks),
        "con_error": len(checks) - len(ok),
        "inflesz_medio_antes": round(sum(c["inflesz_antes"] for c in ok) / count, 1),
        "inflesz_medio_despues": round(sum(c["inflesz_despues"] for c in ok) / count, 1),
        "textos_en_lectura_facil": sum(c["lectura_facil"] for c in ok),
        "errores_reglas": sum(c["errores_reglas"] for c in ok),
        "avisos_reglas": sum(c["avisos_reglas"] for c in ok),
        "textos_con_numeros_perdidos": sum(1 for c in ok if c["numeros_perdidos"]),
        "numeros_perdidos": sum(len(c["numeros_perdidos"]) for c in ok),
        "dias_de_la_semana_equivocados": sum(len(c["dias_equivocados"]) for c in ok),
        "pictogramas_bien": sum(1 for c in ok if c["pictograma"] == "bien"),
        "segundos_medios": round(sum(c["segundos"] for c in ok) / count),
    }


def main() -> None:
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    name = sys.argv[1]
    for results_dir, facts_dir in [(EVALS_DIR / "resultados", EVALS_DIR / "hechos"),
                                   (EVALS_DIR / "resultados-privados", EVALS_DIR / "hechos-privados")]:
        run_dir = results_dir / name
        if not run_dir.exists():
            continue
        checks = []
        for path in sorted(run_dir.glob("*.json")):
            if path.name == "comprobaciones.json" or path.name.startswith("juez"):
                continue
            result = json.loads(path.read_text(encoding="utf-8"))
            facts = json.loads((facts_dir / path.name).read_text(encoding="utf-8"))
            checks.append(check(result, facts))
        total = summary(checks)
        (run_dir / "comprobaciones.json").write_text(
            json.dumps({"resumen": total, "textos": checks}, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8", newline="\n")
        print(f"## {run_dir.relative_to(EVALS_DIR)}\n")
        for key, value in total.items():
            print(f"- {key}: {value}")
        print()
        for c in checks:
            problems = c.get("error") or "; ".join(filter(None, [
                f"números perdidos: {', '.join(c['numeros_perdidos'])}" if c["numeros_perdidos"] else "",
                f"días mal: {', '.join(c['dias_equivocados'])}" if c["dias_equivocados"] else "",
                c["pictograma"] if c["pictograma"] != "bien" else "",
            ])) or "bien"
            print(f"- {c['texto']}: {problems}")


if __name__ == "__main__":
    main()
