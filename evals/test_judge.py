"""Tests de judge.py (sin llamar a la IA). Uso: python -m unittest discover evals"""

import unittest

from judge import parse_answer, summary, user_prompt

FACTS = [
    {"tipo": "horario", "dato": "De martes a domingo"},
    {"tipo": "sueldo", "dato": "19.600 euros brutos al año"},
]


class UserPromptTest(unittest.TestCase):

    def test_los_hechos_van_numerados(self):
        prompt = user_prompt("La oferta", "El texto fácil", FACTS)
        self.assertIn("1. De martes a domingo\n2. 19.600 euros brutos al año", prompt)
        self.assertIn("HECHOS CLAVE (2)", prompt)


class ParseAnswerTest(unittest.TestCase):

    def test_une_cada_veredicto_con_su_hecho(self):
        answer = {"hechos": [{"numero": 2, "cita": "Ganas 19.600 euros.", "diferencias": ["brutos", " "],
                              "veredicto": "cambiado"},
                             {"numero": 1, "cita": "De martes a domingo.", "diferencias": [], "veredicto": "bien"}],
                  "inventado": []}
        judged = parse_answer(answer, FACTS)
        self.assertEqual([f["veredicto"] for f in judged["hechos"]], ["bien", "cambiado"])
        self.assertEqual(judged["hechos"][1]["dato"], "19.600 euros brutos al año")
        self.assertEqual(judged["hechos"][1]["diferencias"], ["brutos"])

    def test_un_hecho_que_la_juez_se_salta_queda_sin_veredicto(self):
        judged = parse_answer({"hechos": [{"numero": 1, "cita": "", "diferencias": [], "veredicto": "bien"}],
                               "inventado": [" ", "Pagan los festivos"]}, FACTS)
        self.assertEqual(judged["hechos"][1]["veredicto"], "sin veredicto")
        self.assertEqual(judged["inventado"], ["Pagan los festivos"])


class SummaryTest(unittest.TestCase):

    def test_cuenta_los_veredictos(self):
        texts = [{"hechos": [{"veredicto": "bien"}, {"veredicto": "falta"}], "inventado": ["algo"]},
                 {"hechos": [{"veredicto": "bien"}, {"veredicto": "cambiado"}], "inventado": []}]
        total = summary(texts)
        self.assertEqual((total["hechos"], total["bien"], total["falta"], total["cambiado"]), (4, 2, 1, 1))
        self.assertEqual(total["porcentaje_bien"], 50.0)
        self.assertEqual(total["textos_con_algo_inventado"], 1)


if __name__ == "__main__":
    unittest.main()
